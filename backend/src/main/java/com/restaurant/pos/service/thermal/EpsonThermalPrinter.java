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
 * Supports USB and Network connections
 */
public class EpsonThermalPrinter {
    
    private static final Logger logger = LoggerFactory.getLogger(EpsonThermalPrinter.class);
    
    private String printerName;
    private String connectionType; // USB, NETWORK
    private String ipAddress;
    private int port;
    
    // Character encoding for Macedonia
    private static final String ENCODING = "CP852"; // Central European encoding
    
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
     * Print kitchen ticket
     */
    public void printKitchenTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing kitchen ticket for order {}", order.getId());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // Header style
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            // Item style
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // Print header
            escpos.write(headerStyle, "========== КУЈНА ==========");
            escpos.feed(1);
            
            escpos.write(itemStyle, "Маса: " + order.getTableNumber());
            escpos.feed(1);
            escpos.write(itemStyle, "Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt()));
            escpos.feed(1);
            escpos.write(itemStyle, "===========================");
            escpos.feed(2);
            
            // Print items
            for (OrderItem item : items) {
                Style quantityStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true);
                
                escpos.write(quantityStyle, item.getQuantity() + "x ");
                escpos.write(itemStyle, item.getMenuItem().getName());
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    Style noteStyle = new Style()
                        .setFontSize(Style.FontSize._1, Style.FontSize._1);
                    escpos.write(noteStyle, "   Забелешка: " + item.getNotes());
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
     * Print bar ticket
     */
    public void printBarTicket(Order order, List<OrderItem> items) throws IOException {
        logger.info("Printing bar ticket for order {}", order.getId());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // Header style
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            // Item style
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // Print header
            escpos.write(headerStyle, "=========== БАР ===========");
            escpos.feed(1);
            
            escpos.write(itemStyle, "Маса: " + order.getTableNumber());
            escpos.feed(1);
            escpos.write(itemStyle, "Време: " + DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt()));
            escpos.feed(1);
            escpos.write(itemStyle, "===========================");
            escpos.feed(2);
            
            // Print items
            for (OrderItem item : items) {
                Style quantityStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setBold(true);
                
                escpos.write(quantityStyle, item.getQuantity() + "x ");
                escpos.write(itemStyle, item.getMenuItem().getName());
                escpos.feed(1);
                
                if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                    Style noteStyle = new Style()
                        .setFontSize(Style.FontSize._1, Style.FontSize._1);
                    escpos.write(noteStyle, "   Забелешка: " + item.getNotes());
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
     * Print receipt
     */
    public void printReceipt(Order order) throws IOException {
        logger.info("Printing receipt for order {}", order.getId());
        
        EscPos escpos = createEscPosInstance();
        if (escpos == null) {
            throw new IOException("Cannot create ESC/POS printer instance");
        }
        
        try {
            // Header style
            Style headerStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            // Restaurant name style
            Style restaurantStyle = new Style()
                .setFontSize(Style.FontSize._3, Style.FontSize._3)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);
            
            // Item style
            Style itemStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Left_Default);
            
            // Total style
            Style totalStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Right)
                .setBold(true);
            
            // Print restaurant header
            escpos.write(restaurantStyle, "РЕСТОРАН POS");
            escpos.feed(2);
            
            // Print order info
            escpos.write(itemStyle, "Маса: " + order.getTableNumber());
            escpos.feed(1);
            escpos.write(itemStyle, "Датум: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(order.getCreatedAt()));
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(2);
            
            // Print items
            for (OrderItem item : order.getItems()) {
                String itemLine = String.format("%dx %-15s %8.2f ден", 
                    item.getQuantity(),
                    truncateString(item.getMenuItem().getName(), 15),
                    item.getTotalPrice());
                escpos.write(itemStyle, itemLine);
                escpos.feed(1);
            }
            
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(1);
            
            // Print total
            String totalLine = String.format("ВКУПНО: %20.2f ден", order.getTotalAmount());
            escpos.write(totalStyle, totalLine);
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(2);
            
            // Print footer
            Style footerStyle = new Style()
                .setJustification(EscPosConst.Justification.Center);
            escpos.write(footerStyle, "Ви благодариме!");
            escpos.feed(1);
            escpos.write(itemStyle, "============================");
            escpos.feed(3);
            escpos.cut(EscPos.CutMode.FULL);
            
        } finally {
            escpos.close();
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