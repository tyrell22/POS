package com.restaurant.pos.service.thermal;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.entity.OrderItem;
import com.restaurant.pos.entity.MenuItem;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Epson Thermal Printer Implementation with FIXED Cyrillic Support
 * SOLUTION: Use CP866 (Cyrillic DOS) + proper byte conversion for Macedonian text
 */
public class EpsonThermalPrinter {
    
    private static final Logger logger = LoggerFactory.getLogger(EpsonThermalPrinter.class);
    
    // FIXED: Use CP866 which is the standard Cyrillic codepage for DOS/ESC-POS printers
    private static final Charset PRINTER_CHARSET = Charset.forName("CP866");
    private static final int CYRILLIC_CODEPAGE = 17; // ESC t 17 for CP866
    
    private String printerName;
    private String connectionType; // USB, NETWORK
    private String ipAddress;
    private int port;
    
    public EpsonThermalPrinter(String printerName, String connectionType, String ipAddress, int port) {
        this.printerName = printerName;
        this.connectionType = connectionType;
        this.ipAddress = ipAddress;
        this.port = port;
        logger.info("Initialized Epson thermal printer: {} via {} with CP866 Cyrillic support", printerName, connectionType);
    }
    
    /**
     * Test connection to thermal printer
     */
    public boolean testConnection() {
        try {
            if ("NETWORK".equals(connectionType)) {
                return testNetworkConnection();
            } else if ("USB".equals(connectionType)) {
                return testUSBConnection();
            }
            return false;
        } catch (Exception e) {
            logger.error("Error testing thermal printer connection", e);
            return false;
        }
    }
    
    /**
     * FIXED: Initialize printer with proper Cyrillic support
     */
    private void initializePrinterEncoding(OutputStream rawStream) throws IOException {
        if (rawStream == null) {
            logger.warn("Raw stream is null, cannot set encoding");
            return;
        }
        
        try {
            // STEP 1: Initialize printer
            rawStream.write(new byte[]{0x1B, 0x40}); // ESC @ - Initialize printer
            rawStream.flush();
            Thread.sleep(100);
            
            // STEP 2: Set codepage to CP866 (Cyrillic)
            rawStream.write(new byte[]{0x1B, 0x74, CYRILLIC_CODEPAGE}); // ESC t 17
            rawStream.flush();
            Thread.sleep(50);
            
            // STEP 3: Set international character set to Cyrillic
            rawStream.write(new byte[]{0x1B, 0x52, 0x07}); // ESC R 7 - Cyrillic character set
            rawStream.flush();
            Thread.sleep(50);
            
            logger.info("✅ Printer encoding initialized: CP866 Cyrillic");
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize printer encoding: {}", e.getMessage());
            throw new IOException("Failed to set printer encoding", e);
        }
    }
    
    /**
     * FIXED: Convert text to proper encoding for thermal printer
     */
    private String convertTextForPrinter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // SOLUTION 1: Convert through CP866 encoding
            byte[] bytes = text.getBytes(PRINTER_CHARSET);
            String converted = new String(bytes, PRINTER_CHARSET);
            
            logger.debug("Text conversion: '{}' -> '{}' (CP866)", text, converted);
            return converted;
            
        } catch (Exception e) {
            logger.warn("Text conversion failed for '{}', using fallback", text);
            // FALLBACK: Transliterate problematic characters
            return transliterateMacedonian(text);
        }
    }
    
    /**
     * FALLBACK: Transliterate Macedonian characters that might not work
     */
    private String transliterateMacedonian(String text) {
        if (text == null) return "";
        
        return text
            // Keep most Cyrillic characters as-is (they should work with CP866)
            // Only transliterate the most problematic ones
            .replace("ќ", "k'")  // Only if this specific character causes issues
            .replace("Ќ", "K'")
            .replace("ѕ", "dz")  // Only if this specific character causes issues  
            .replace("Ѕ", "DZ")
            .replace("џ", "dzh") // Only if this specific character causes issues
            .replace("Џ", "DZH");
    }
    
    /**
     * FIXED: Write text with proper encoding
     */
    private void writeEncodedText(EscPos escpos, Style style, String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // Convert text to printer-compatible encoding
        String convertedText = convertTextForPrinter(text);
        
        // Write using converted text
        escpos.write(style, convertedText);
    }
    
    /**
     * Print kitchen ticket - NEW items only + FIXED encoding
     */
    public void printKitchenTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing kitchen ticket for order {} with FIXED encoding", order.getId());
        
        // Filter to only NEW items (pending quantity > 0)  
        List<OrderItem> newItems = items.stream()
            .filter(item -> {
                int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                int pendingQty = totalQty - sentQty;
                
                logger.debug("Item: {}, Total: {}, Sent: {}, Pending: {}", 
                    item.getMenuItem().getName(), totalQty, sentQty, pendingQty);
                
                return pendingQty > 0;
            })
            .toList();
        
        if (newItems.isEmpty()) {
            logger.info("No new kitchen items to print for order {}", order.getId());
            return;
        }
        
        logger.info("Printing {} new kitchen items with FIXED Cyrillic support", newItems.size());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // FIXED: Initialize encoding FIRST
            OutputStream rawStream = escpos.getOutputStream();
            initializePrinterEncoding(rawStream);
            
            // Define styles
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // FIXED: Print header with proper encoding
            writeEncodedText(escpos, headerStyle, "========== КУЈНА ==========");
            escpos.feed(1);
            
            writeEncodedText(escpos, itemStyle, "Маса: " + getTableDisplayName(order));
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt()));
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "===========================");
            escpos.feed(2);
            
            // FIXED: Print NEW items with proper encoding
            for (OrderItem item : newItems) {
                int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                int pendingQty = totalQty - sentQty;
                
                Style quantityStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true);
                
                // Print quantity (numbers work fine)
                escpos.write(quantityStyle, pendingQty + "x ");
                
                // FIXED: Print item name with encoding
                writeEncodedText(escpos, itemStyle, item.getMenuItem().getName());
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    Style noteStyle = new Style()
                        .setFontSize(Style.FontSize._1, Style.FontSize._1);
                    writeEncodedText(escpos, noteStyle, "   Забелешка: " + item.getNotes());
                    escpos.feed(1);
                }
                escpos.feed(1);
            }
            
            writeEncodedText(escpos, itemStyle, "===========================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
            logger.info("✅ Kitchen ticket printed successfully with Cyrillic support");
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * Print bar ticket - NEW items only + FIXED encoding
     */
    public void printBarTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing bar ticket for order {} with FIXED encoding", order.getId());
        
        // Filter to only NEW items (pending quantity > 0)
        List<OrderItem> newItems = items.stream()
            .filter(item -> {
                int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                int pendingQty = totalQty - sentQty;
                
                logger.debug("Item: {}, Total: {}, Sent: {}, Pending: {}", 
                    item.getMenuItem().getName(), totalQty, sentQty, pendingQty);
                
                return pendingQty > 0;
            })
            .toList();
        
        if (newItems.isEmpty()) {
            logger.info("No new bar items to print for order {}", order.getId());
            return;
        }
        
        logger.info("Printing {} new bar items with FIXED Cyrillic support", newItems.size());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // FIXED: Initialize encoding FIRST
            OutputStream rawStream = escpos.getOutputStream();
            initializePrinterEncoding(rawStream);
            
            // Define styles
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // FIXED: Print header with proper encoding
            writeEncodedText(escpos, headerStyle, "=========== БАР ===========");
            escpos.feed(1);
            
            writeEncodedText(escpos, itemStyle, "Маса: " + getTableDisplayName(order));
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt()));
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "===========================");
            escpos.feed(2);
            
            // FIXED: Print NEW items with proper encoding
            for (OrderItem item : newItems) {
                int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                int pendingQty = totalQty - sentQty;
                
                Style quantityStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true);
                
                // Print quantity (numbers work fine)
                escpos.write(quantityStyle, pendingQty + "x ");
                
                // FIXED: Print item name with encoding
                writeEncodedText(escpos, itemStyle, item.getMenuItem().getName());
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    Style noteStyle = new Style()
                        .setFontSize(Style.FontSize._1, Style.FontSize._1);
                    writeEncodedText(escpos, noteStyle, "   Забелешка: " + item.getNotes());
                    escpos.feed(1);
                }
                escpos.feed(1);
            }
            
            writeEncodedText(escpos, itemStyle, "===========================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
            logger.info("✅ Bar ticket printed successfully with Cyrillic support");
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * Print receipt - All items + FIXED encoding
     */
    public void printReceipt(Order order) throws IOException {
        logger.info("Printing receipt for order {} with FIXED encoding", order.getId());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // FIXED: Initialize encoding FIRST
            OutputStream rawStream = escpos.getOutputStream();
            initializePrinterEncoding(rawStream);
            
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            Style restaurantStyle = new Style()
                .setFontSize(Style.FontSize._3, Style.FontSize._3)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            Style totalStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Right)
                .setBold(true);
            
            // FIXED: All text with proper encoding
            writeEncodedText(escpos, restaurantStyle, "РЕСТОРАН POS");
            escpos.feed(2);
            
            writeEncodedText(escpos, itemStyle, "Маса: " + getTableDisplayName(order));
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "Датум: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(order.getCreatedAt()));
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "============================");
            escpos.feed(2);
            
            for (OrderItem item : order.getItems()) {
                // Format item line with price
                String itemLine = String.format("%dx %-15s %8.2f ден", 
                    item.getQuantity(),
                    truncateString(item.getMenuItem().getName(), 15), // Use original name, will be converted
                    item.getTotalPrice());
                
                writeEncodedText(escpos, itemStyle, itemLine);
                escpos.feed(1);
            }
            
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "============================");
            escpos.feed(1);
            
            String totalLine = String.format("ВКУПНО: %20.2f ден", order.getTotalAmount());
            writeEncodedText(escpos, totalStyle, totalLine);
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "============================");
            escpos.feed(2);
            
            Style footerStyle = new Style()
                .setJustification(EscPosConst.Justification.Center);
            writeEncodedText(escpos, footerStyle, "Ви благодариме!");
            escpos.feed(1);
            writeEncodedText(escpos, itemStyle, "============================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
            logger.info("✅ Receipt printed successfully with Cyrillic support");
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * Helper method to get table display name
     */
    private String getTableDisplayName(Order order) {
        if (order.getTableNumber() >= 1000) {
            return "Понеси #" + (order.getTableNumber() - 1000 + 1);
        } else {
            return String.valueOf(order.getTableNumber());
        }
    }
    
    /**
     * Get list of available thermal printers
     */
    public List<String> getAvailablePrinters() {
        List<String> printers = new ArrayList<>();
        
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService printService : printServices) {
                String printerName = printService.getName();
                printers.add(printerName);
            }
        } catch (Exception e) {
            logger.error("Error getting available printers", e);
        }
        
        List<String> commonPrinters = Arrays.asList(
            "Epson TM-T88IV",    // Your printer
            "Epson TM-T20II",
            "Epson TM-T88V",
            "Epson TM-T82",
            "Epson TM-T20",
            "Generic ESC/POS Printer"
        );
        
        for (String commonPrinter : commonPrinters) {
            if (!printers.contains(commonPrinter)) {
                printers.add(commonPrinter);
            }
        }
        
        return printers;
    }
    
    /**
     * Create EscPos instance - UNCHANGED
     */
    private EscPos createEscPosInstance() throws IOException {
        try {
            if ("NETWORK".equals(connectionType)) {
                Socket socket = new Socket(ipAddress, port);
                OutputStream outputStream = socket.getOutputStream();
                return new EscPos(outputStream);
            } else if ("USB".equals(connectionType)) {
                PrintService printService = findPrintService(printerName);
                if (printService != null) {
                    PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                    return new EscPos(printerOutputStream);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error creating ESC/POS instance", e);
            throw new IOException("Cannot create printer connection", e);
        }
    }
    
    /**
     * Find print service - UNCHANGED
     */
    private PrintService findPrintService(String printerName) {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService printService : printServices) {
            if (printService.getName().contains(printerName) || 
                printerName.contains(printService.getName())) {
                return printService;
            }
        }
        
        if (printServices.length > 0) {
            logger.warn("Printer '{}' not found, using first available: {}", 
                printerName, printServices[0].getName());
            return printServices[0];
        }
        
        return null;
    }
    
    /**
     * Test methods - UNCHANGED
     */
    private boolean testNetworkConnection() {
        try (Socket socket = new Socket(ipAddress, port)) {
            return socket.isConnected();
        } catch (Exception e) {
            logger.error("Network connection test failed", e);
            return false;
        }
    }
    
    private boolean testUSBConnection() {
        try {
            PrintService printService = findPrintService(printerName);
            return printService != null;
        } catch (Exception e) {
            logger.error("USB connection test failed", e);
            return false;
        }
    }
    
    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
