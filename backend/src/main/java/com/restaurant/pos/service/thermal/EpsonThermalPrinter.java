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

/**
 * Epson Thermal Printer Implementation using ESC/POS commands
 * WORKING: Original connection + Fixed Macedonian encoding + New items only
 */
public class EpsonThermalPrinter {
    
    private static final Logger logger = LoggerFactory.getLogger(EpsonThermalPrinter.class);
    
    private String printerName;
    private String connectionType; // USB, NETWORK
    private String ipAddress;
    private int port;
    
    public EpsonThermalPrinter(String printerName, String connectionType, String ipAddress, int port) {
        this.printerName = printerName;
        this.connectionType = connectionType;
        this.ipAddress = ipAddress;
        this.port = port;
        logger.info("Initialized Epson thermal printer: {} via {}", printerName, connectionType);
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
     * Print kitchen ticket - NEW items only + Fixed encoding
     */
    public void printKitchenTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing kitchen ticket for order {}", order.getId());
        
        // FIXED: Filter to only NEW items (pending quantity > 0)  
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
        
        logger.info("Printing {} new kitchen items", newItems.size());
        
        // ORIGINAL connection logic
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // ENCODING FIX: Set codepage for Cyrillic/Macedonian
            try {
                OutputStream rawStream = escpos.getOutputStream();
                if (rawStream != null) {
                    // ESC t 17 for Windows-1251 (Cyrillic) - works better than CP852
                    rawStream.write(new byte[]{0x1B, 0x74, 17});
                    rawStream.flush();
                    logger.debug("Set codepage to Windows-1251 for Macedonian text");
                }
            } catch (Exception e) {
                logger.warn("Could not set codepage, continuing with default: {}", e.getMessage());
            }
            
            // Original printing styles
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // Print header with encoding fix
            escpos.write(headerStyle, fixMacedonianText("========== КУЈНА =========="));
            escpos.feed(1);
            
            escpos.write(itemStyle, fixMacedonianText("Маса: " + getTableDisplayName(order)));
            escpos.feed(1);
            escpos.write(itemStyle, fixMacedonianText("Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt())));
            escpos.feed(1);
            escpos.write(itemStyle, "===========================");
            escpos.feed(2);
            
            // Print NEW items with fixed encoding
            for (OrderItem item : newItems) {
                int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                int pendingQty = totalQty - sentQty;
                
                Style quantityStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true);
                
                escpos.write(quantityStyle, pendingQty + "x ");
                escpos.write(itemStyle, fixMacedonianText(item.getMenuItem().getName()));
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    Style noteStyle = new Style()
                        .setFontSize(Style.FontSize._1, Style.FontSize._1);
                    escpos.write(noteStyle, fixMacedonianText("   Забелешка: " + item.getNotes()));
                    escpos.feed(1);
                }
                escpos.feed(1);
            }
            
            escpos.write(itemStyle, "===========================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * Print bar ticket - NEW items only + Fixed encoding
     */
    public void printBarTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing bar ticket for order {}", order.getId());
        
        // FIXED: Filter to only NEW items (pending quantity > 0)
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
        
        logger.info("Printing {} new bar items", newItems.size());
        
        // ORIGINAL connection logic
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // ENCODING FIX: Set codepage for Cyrillic/Macedonian
            try {
                OutputStream rawStream = escpos.getOutputStream();
                if (rawStream != null) {
                    // ESC t 17 for Windows-1251 (Cyrillic)
                    rawStream.write(new byte[]{0x1B, 0x74, 17});
                    rawStream.flush();
                    logger.debug("Set codepage to Windows-1251 for Macedonian text");
                }
            } catch (Exception e) {
                logger.warn("Could not set codepage, continuing with default: {}", e.getMessage());
            }
            
            // Original printing styles
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // Print header with encoding fix
            escpos.write(headerStyle, fixMacedonianText("=========== БАР ==========="));
            escpos.feed(1);
            
            escpos.write(itemStyle, fixMacedonianText("Маса: " + getTableDisplayName(order)));
            escpos.feed(1);
            escpos.write(itemStyle, fixMacedonianText("Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt())));
            escpos.feed(1);
            escpos.write(itemStyle, "===========================");
            escpos.feed(2);
            
            // Print NEW items with fixed encoding
            for (OrderItem item : newItems) {
                int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                int pendingQty = totalQty - sentQty;
                
                Style quantityStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true);
                
                escpos.write(quantityStyle, pendingQty + "x ");
                escpos.write(itemStyle, fixMacedonianText(item.getMenuItem().getName()));
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    Style noteStyle = new Style()
                        .setFontSize(Style.FontSize._1, Style.FontSize._1);
                    escpos.write(noteStyle, fixMacedonianText("   Забелешка: " + item.getNotes()));
                    escpos.feed(1);
                }
                escpos.feed(1);
            }
            
            escpos.write(itemStyle, "===========================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * Print receipt - All items + Fixed encoding
     */
    public void printReceipt(Order order) throws IOException {
        logger.info("Printing receipt for order {}", order.getId());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // ENCODING FIX: Set codepage for Cyrillic/Macedonian
            try {
                OutputStream rawStream = escpos.getOutputStream();
                if (rawStream != null) {
                    // ESC t 17 for Windows-1251 (Cyrillic)
                    rawStream.write(new byte[]{0x1B, 0x74, 17});
                    rawStream.flush();
                    logger.debug("Set codepage to Windows-1251 for Macedonian text");
                }
            } catch (Exception e) {
                logger.warn("Could not set codepage, continuing with default: {}", e.getMessage());
            }
            
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
            
            escpos.write(restaurantStyle, fixMacedonianText("РЕСТОРАН POS"));
            escpos.feed(2);
            
            escpos.write(itemStyle, fixMacedonianText("Маса: " + getTableDisplayName(order)));
            escpos.feed(1);
            escpos.write(itemStyle, fixMacedonianText("Датум: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(order.getCreatedAt())));
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(2);
            
            for (OrderItem item : order.getItems()) {
                String itemLine = String.format("%dx %-15s %8.2f ден", 
                    item.getQuantity(),
                    truncateString(fixMacedonianText(item.getMenuItem().getName()), 15),
                    item.getTotalPrice());
                escpos.write(itemStyle, itemLine);
                escpos.feed(1);
            }
            
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(1);
            
            String totalLine = String.format("ВКУПНО: %20.2f ден", order.getTotalAmount());
            escpos.write(totalStyle, fixMacedonianText(totalLine));
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(2);
            
            Style footerStyle = new Style()
                .setJustification(EscPosConst.Justification.Center);
            escpos.write(footerStyle, fixMacedonianText("Ви благодариме!"));
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
        } finally {
            escpos.close();
        }
    }
    
    /**
     * ENCODING FIX: Handle problematic Macedonian characters
     */
    private String fixMacedonianText(String text) {
        if (text == null) return "";
        
        // Convert problematic Macedonian characters to printer-friendly alternatives
        // Only convert the ones that commonly cause issues
        return text
            .replace("ќ", "k'")  // ќ -> k' if it shows as ?
            .replace("Ќ", "K'")  // Ќ -> K'
            .replace("ѕ", "z'")  // ѕ -> z' if it shows as ?
            .replace("Ѕ", "Z'")  // Ѕ -> Z'
            .replace("џ", "dz")  // џ -> dz if it shows as ?
            .replace("Џ", "DZ"); // Џ -> DZ
        
        // Note: Keep other Cyrillic characters as-is since Windows-1251 should support them
        // Characters like а, б, в, г, д, е, ж, з, и, ј, к, л, м, н, о, п, р, с, т, у, ф, х, ц, ч, ш should work
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
     * ORIGINAL connection logic - UNCHANGED
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
     * ORIGINAL find print service - UNCHANGED
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
     * ORIGINAL test methods - UNCHANGED
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
