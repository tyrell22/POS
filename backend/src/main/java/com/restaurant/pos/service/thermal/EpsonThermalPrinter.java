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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.charset.Charset;

/**
 * Epson Thermal Printer Implementation using ESC/POS commands
 * FIXED: Proper encoding for Macedonian text and only print new items
 */
public class EpsonThermalPrinter {
    
    private static final Logger logger = LoggerFactory.getLogger(EpsonThermalPrinter.class);
    
    private String printerName;
    private String connectionType; // USB, NETWORK
    private String ipAddress;
    private int port;
    
    // FIXED: Use Windows-1251 (Cyrillic) encoding for Macedonian text
    private static final String ENCODING = "windows-1251"; // Better for Cyrillic
    private static final Charset CHARSET = Charset.forName("windows-1251");
    
    public EpsonThermalPrinter(String printerName, String connectionType, String ipAddress, int port) {
        this.printerName = printerName;
        this.connectionType = connectionType;
        this.ipAddress = ipAddress;
        this.port = port;
        logger.info("Initialized Epson thermal printer: {} via {} with encoding: {}", printerName, connectionType, ENCODING);
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
     * FIXED: Print kitchen ticket - only NEW (unsent) items
     */
    public void printKitchenTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing kitchen ticket for order {} with {} items", order.getId(), items.size());
        
        // FIXED: Filter to only NEW items (pending quantity > 0)
        List<OrderItem> newItems = items.stream()
            .filter(item -> {
                int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                return pendingQty > 0;
            })
            .toList();
        
        if (newItems.isEmpty()) {
            logger.info("No new kitchen items to print for order {}", order.getId());
            return;
        }
        
        logger.info("Printing {} new kitchen items", newItems.size());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // FIXED: Send codepage command for Cyrillic
            escpos.write(new byte[]{0x1B, 0x74, 0x11}); // ESC t 17 (CP1251)
            
            // Header
            printCenteredText(escpos, "========== КУЈНА ==========", true);
            escpos.feed(1);
            
            printLeftText(escpos, "Маса: " + getTableDisplayName(order), false);
            escpos.feed(1);
            printLeftText(escpos, "Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt()), false);
            escpos.feed(1);
            printLeftText(escpos, "===========================", false);
            escpos.feed(2);
            
            // FIXED: Print only NEW items with their pending quantities
            for (OrderItem item : newItems) {
                int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                
                // Large quantity
                printLeftText(escpos, pendingQty + "x ", true, true); // Bold and large
                // Item name on same line
                printLeftText(escpos, cleanMacedonianText(item.getMenuItem().getName()), true, false);
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    printLeftText(escpos, "   Забелешка: " + cleanMacedonianText(item.getNotes()), false);
                    escpos.feed(1);
                }
                escpos.feed(1);
            }
            
            printLeftText(escpos, "===========================", false);
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
            logger.info("Successfully printed kitchen ticket with {} new items", newItems.size());
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * FIXED: Print bar ticket - only NEW (unsent) items
     */
    public void printBarTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing bar ticket for order {} with {} items", order.getId(), items.size());
        
        // FIXED: Filter to only NEW items (pending quantity > 0)
        List<OrderItem> newItems = items.stream()
            .filter(item -> {
                int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                return pendingQty > 0;
            })
            .toList();
        
        if (newItems.isEmpty()) {
            logger.info("No new bar items to print for order {}", order.getId());
            return;
        }
        
        logger.info("Printing {} new bar items", newItems.size());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // FIXED: Send codepage command for Cyrillic
            escpos.write(new byte[]{0x1B, 0x74, 0x11}); // ESC t 17 (CP1251)
            
            // Header
            printCenteredText(escpos, "=========== БАР ===========", true);
            escpos.feed(1);
            
            printLeftText(escpos, "Маса: " + getTableDisplayName(order), false);
            escpos.feed(1);
            printLeftText(escpos, "Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt()), false);
            escpos.feed(1);
            printLeftText(escpos, "===========================", false);
            escpos.feed(2);
            
            // FIXED: Print only NEW items with their pending quantities
            for (OrderItem item : newItems) {
                int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                
                // Large quantity
                printLeftText(escpos, pendingQty + "x ", true, true); // Bold and large
                // Item name on same line
                printLeftText(escpos, cleanMacedonianText(item.getMenuItem().getName()), true, false);
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    printLeftText(escpos, "   Забелешка: " + cleanMacedonianText(item.getNotes()), false);
                    escpos.feed(1);
                }
                escpos.feed(1);
            }
            
            printLeftText(escpos, "===========================", false);
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
            logger.info("Successfully printed bar ticket with {} new items", newItems.size());
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * Print receipt (unchanged, prints all items)
     */
    public void printReceipt(Order order) throws IOException {
        logger.info("Printing receipt for order {}", order.getId());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // FIXED: Send codepage command for Cyrillic
            escpos.write(new byte[]{0x1B, 0x74, 0x11}); // ESC t 17 (CP1251)
            
            // Restaurant header
            printCenteredText(escpos, "РЕСТОРАН POS", true, true); // Large and bold
            escpos.feed(2);
            
            // Order info
            printLeftText(escpos, "Маса: " + getTableDisplayName(order), false);
            escpos.feed(1);
            printLeftText(escpos, "Датум: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(order.getCreatedAt()), false);
            escpos.feed(1);
            printLeftText(escpos, "============================", false);
            escpos.feed(2);
            
            // Print all items
            for (OrderItem item : order.getItems()) {
                String itemLine = String.format("%dx %-15s %8.2f ден", 
                    item.getQuantity(),
                    truncateString(cleanMacedonianText(item.getMenuItem().getName()), 15),
                    item.getTotalPrice());
                printLeftText(escpos, itemLine, false);
                escpos.feed(1);
            }
            
            escpos.feed(1);
            printLeftText(escpos, "============================", false);
            escpos.feed(1);
            
            // Total
            String totalLine = String.format("ВКУПНО: %20.2f ден", order.getTotalAmount());
            printRightText(escpos, totalLine, true); // Bold total
            escpos.feed(1);
            printLeftText(escpos, "============================", false);
            escpos.feed(2);
            
            // Footer
            printCenteredText(escpos, "Ви благодариме!", false);
            escpos.feed(1);
            printLeftText(escpos, "============================", false);
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
            logger.info("Successfully printed receipt");
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * FIXED: Helper method to get table display name
     */
    private String getTableDisplayName(Order order) {
        if (order.getTableNumber() >= 1000) {
            return "Понеси #" + (order.getTableNumber() - 1000 + 1);
        } else {
            return String.valueOf(order.getTableNumber());
        }
    }
    
    /**
     * FIXED: Clean and convert Macedonian text for proper printing
     */
    private String cleanMacedonianText(String text) {
        if (text == null) return "";
        
        // Convert common Macedonian characters that might not print correctly
        String cleaned = text
            .replace("ќ", "k'")  // If ќ doesn't work
            .replace("Ќ", "K'")
            .replace("ѕ", "z'")  // If ѕ doesn't work  
            .replace("Ѕ", "Z'")
            .replace("џ", "dz")  // If џ doesn't work
            .replace("Џ", "DZ");
        
        // Remove any remaining non-printable characters
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        return cleaned;
    }
    
    /**
     * FIXED: Helper methods for proper text printing with encoding
     */
    private void printCenteredText(EscPos escpos, String text, boolean bold) throws IOException {
        printCenteredText(escpos, text, bold, false);
    }
    
    private void printCenteredText(EscPos escpos, String text, boolean bold, boolean large) throws IOException {
        Style style = new Style()
            .setJustification(EscPosConst.Justification.Center);
        
        if (bold) style = style.setBold(true);
        if (large) style = style.setFontSize(Style.FontSize._2, Style.FontSize._2);
        
        // Convert text to proper encoding
        byte[] textBytes = cleanMacedonianText(text).getBytes(CHARSET);
        escpos.write(style, new String(textBytes, CHARSET));
    }
    
    private void printLeftText(EscPos escpos, String text, boolean bold) throws IOException {
        printLeftText(escpos, text, bold, false);
    }
    
    private void printLeftText(EscPos escpos, String text, boolean bold, boolean large) throws IOException {
        Style style = new Style()
            .setJustification(EscPosConst.Justification.Left_Default);
        
        if (bold) style = style.setBold(true);
        if (large) style = style.setFontSize(Style.FontSize._2, Style.FontSize._2);
        
        // Convert text to proper encoding
        byte[] textBytes = cleanMacedonianText(text).getBytes(CHARSET);
        escpos.write(style, new String(textBytes, CHARSET));
    }
    
    private void printRightText(EscPos escpos, String text, boolean bold) throws IOException {
        Style style = new Style()
            .setJustification(EscPosConst.Justification.Right);
        
        if (bold) style = style.setBold(true);
        
        // Convert text to proper encoding
        byte[] textBytes = cleanMacedonianText(text).getBytes(CHARSET);
        escpos.write(style, new String(textBytes, CHARSET));
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
        
        // Add common thermal printer names as fallback
        List<String> commonPrinters = Arrays.asList(
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
     * Create ESC/POS instance based on connection type
     */
    private EscPos createEscPosInstance() throws IOException {
        try {
            if ("NETWORK".equals(connectionType)) {
                // Create network connection using raw socket
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
     * Find print service by name
     */
    private PrintService findPrintService(String printerName) {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService printService : printServices) {
            if (printService.getName().contains(printerName) || 
                printerName.contains(printService.getName())) {
                return printService;
            }
        }
        
        // Return first available service as fallback
        if (printServices.length > 0) {
            logger.warn("Printer '{}' not found, using first available: {}", 
                printerName, printServices[0].getName());
            return printServices[0];
        }
        
        return null;
    }
    
    /**
     * Test network connection
     */
    private boolean testNetworkConnection() {
        try (Socket socket = new Socket(ipAddress, port)) {
            return socket.isConnected();
        } catch (Exception e) {
            logger.error("Network connection test failed", e);
            return false;
        }
    }
    
    /**
     * Test USB connection
     */
    private boolean testUSBConnection() {
        try {
            PrintService printService = findPrintService(printerName);
            return printService != null;
        } catch (Exception e) {
            logger.error("USB connection test failed", e);
            return false;
        }
    }
    
    /**
     * Truncate string to specified length
     */
    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}