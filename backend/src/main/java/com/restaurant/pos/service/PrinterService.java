package com.restaurant.pos.service;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.entity.OrderItem;
import com.restaurant.pos.entity.MenuItem;
import com.restaurant.pos.service.fiscal.MacedonianFiscalPrinter;
import com.restaurant.pos.service.thermal.EpsonThermalPrinter;
import com.restaurant.pos.dto.PrinterConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PrinterService {
    
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);
    private static final String ADMIN_CODE = "1234";
    
    // Printer configuration
    @Value("${printer.fiscal.enabled:true}")
    private boolean fiscalPrinterEnabled;
    
    @Value("${printer.fiscal.port:LPT1}") // FIXED: Default to LPT1
    private String fiscalPrinterPort;
    
    @Value("${printer.fiscal.type:SYNERGY}")
    private String fiscalPrinterType;
    
    @Value("${printer.thermal.enabled:true}")
    private boolean thermalPrinterEnabled;
    
    @Value("${printer.thermal.name:Epson TM-T20II}")
    private String thermalPrinterName;
    
    @Value("${printer.thermal.connection:USB}")
    private String thermalConnection;
    
    @Value("${printer.thermal.ip:192.168.1.100}")
    private String thermalPrinterIP;
    
    @Value("${printer.thermal.port:9100}")
    private int thermalPrinterPort;
    
    // Printer instances
    private MacedonianFiscalPrinter fiscalPrinter;
    private EpsonThermalPrinter thermalPrinter;
    
    // Initialize printers
    public void initializePrinters() {
        try {
            // Initialize fiscal printer
            if (fiscalPrinterEnabled) {
                fiscalPrinter = new MacedonianFiscalPrinter(fiscalPrinterPort, fiscalPrinterType);
                logger.info("Fiscal printer initialized: {} on {}", fiscalPrinterType, fiscalPrinterPort);
            }
            
            // Initialize thermal printer
            if (thermalPrinterEnabled) {
                thermalPrinter = new EpsonThermalPrinter(thermalPrinterName, thermalConnection, thermalPrinterIP, thermalPrinterPort);
                logger.info("Thermal printer initialized: {} via {}", thermalPrinterName, thermalConnection);
            }
        } catch (Exception e) {
            logger.error("Error initializing printers", e);
        }
    }
    
    /**
     * FIXED: Print order tickets to kitchen/bar using thermal printer
     * Now only prints NEW (unsent) items
     */
    public void printOrderTickets(Order order) {
        try {
            if (!thermalPrinterEnabled || thermalPrinter == null) {
                logger.warn("Thermal printer is disabled or not initialized");
                printSimulatedTickets(order);
                return;
            }
            
            // FIXED: Separate NEW items by print destination
            List<OrderItem> newKitchenItems = order.getItems().stream()
                .filter(item -> {
                    // Check if it's a kitchen item
                    boolean isKitchen = item.getMenuItem().getPrintDestination() == MenuItem.PrintDestination.КУЈНА;
                    // Check if it has pending (unsent) quantity
                    int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                    return isKitchen && pendingQty > 0;
                })
                .toList();
            
            List<OrderItem> newBarItems = order.getItems().stream()
                .filter(item -> {
                    // Check if it's a bar item
                    boolean isBar = item.getMenuItem().getPrintDestination() == MenuItem.PrintDestination.БАР;
                    // Check if it has pending (unsent) quantity
                    int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                    return isBar && pendingQty > 0;
                })
                .toList();
            
            logger.info("Printing tickets for order {}: {} new kitchen items, {} new bar items", 
                order.getId(), newKitchenItems.size(), newBarItems.size());
            
            // Print kitchen ticket if there are NEW kitchen items
            if (!newKitchenItems.isEmpty()) {
                thermalPrinter.printKitchenTicket(order, newKitchenItems);
                logger.info("Kitchen ticket printed for order {} with {} new items", order.getId(), newKitchenItems.size());
            }
            
            // Print bar ticket if there are NEW bar items
            if (!newBarItems.isEmpty()) {
                thermalPrinter.printBarTicket(order, newBarItems);
                logger.info("Bar ticket printed for order {} with {} new items", order.getId(), newBarItems.size());
            }
            
            // Log if no new items to print
            if (newKitchenItems.isEmpty() && newBarItems.isEmpty()) {
                logger.info("No new items to print for order {}", order.getId());
            }
            
        } catch (Exception e) {
            logger.error("Error printing order tickets for order {}", order.getId(), e);
            // Fallback to simulated printing
            printSimulatedTickets(order);
        }
    }
    
    /**
     * Print fiscal receipt using fiscal printer (when F key is held)
     */
    public void printFiscalReceipt(Order order) {
        try {
            if (!fiscalPrinterEnabled || fiscalPrinter == null) {
                logger.warn("Fiscal printer is disabled or not initialized");
                printSimulatedReceipt(order);
                return;
            }
            
            fiscalPrinter.printFiscalReceipt(order);
            logger.info("Fiscal receipt printed for order {}", order.getId());
            
        } catch (Exception e) {
            logger.error("Error printing fiscal receipt for order {}", order.getId(), e);
            // Fallback to simulated printing
            printSimulatedReceipt(order);
        }
    }
    
    /**
     * Print regular receipt using thermal printer (default)
     */
    public void printReceipt(Order order) {
        try {
            if (!thermalPrinterEnabled || thermalPrinter == null) {
                logger.warn("Thermal printer is disabled or not initialized");
                printSimulatedReceipt(order);
                return;
            }
            
            thermalPrinter.printReceipt(order);
            logger.info("Thermal receipt printed for order {}", order.getId());
            
        } catch (Exception e) {
            logger.error("Error printing thermal receipt for order {}", order.getId(), e);
            // Fallback to simulated printing
            printSimulatedReceipt(order);
        }
    }
    
    /**
     * Test printer connections
     */
    public Map<String, Object> testPrinterConnections() {
        Map<String, Object> results = new HashMap<>();
        
        // Test fiscal printer
        Map<String, Object> fiscalResult = new HashMap<>();
        if (fiscalPrinterEnabled) {
            try {
                if (fiscalPrinter == null) {
                    fiscalPrinter = new MacedonianFiscalPrinter(fiscalPrinterPort, fiscalPrinterType);
                }
                boolean fiscalConnected = fiscalPrinter.testConnection();
                fiscalResult.put("connected", fiscalConnected);
                fiscalResult.put("status", fiscalConnected ? "Поврзан" : "Неповрзан");
                fiscalResult.put("port", fiscalPrinterPort);
                fiscalResult.put("type", fiscalPrinterType);
                
                // FIXED: Additional info for parallel ports
                if (fiscalPrinterPort.toUpperCase().startsWith("LPT")) {
                    fiscalResult.put("connectionType", "Parallel Port");
                } else {
                    fiscalResult.put("connectionType", "Serial Port");
                }
            } catch (Exception e) {
                fiscalResult.put("connected", false);
                fiscalResult.put("status", "Грешка: " + e.getMessage());
                fiscalResult.put("port", fiscalPrinterPort);
                fiscalResult.put("type", fiscalPrinterType);
            }
        } else {
            fiscalResult.put("connected", false);
            fiscalResult.put("status", "Оневозможен");
        }
        results.put("fiscal", fiscalResult);
        
        // Test thermal printer
        Map<String, Object> thermalResult = new HashMap<>();
        if (thermalPrinterEnabled) {
            try {
                if (thermalPrinter == null) {
                    thermalPrinter = new EpsonThermalPrinter(thermalPrinterName, thermalConnection, thermalPrinterIP, thermalPrinterPort);
                }
                boolean thermalConnected = thermalPrinter.testConnection();
                thermalResult.put("connected", thermalConnected);
                thermalResult.put("status", thermalConnected ? "Поврзан" : "Неповрзан");
                thermalResult.put("name", thermalPrinterName);
                thermalResult.put("connection", thermalConnection);
                if ("NETWORK".equals(thermalConnection)) {
                    thermalResult.put("ip", thermalPrinterIP);
                    thermalResult.put("port", thermalPrinterPort);
                }
            } catch (Exception e) {
                thermalResult.put("connected", false);
                thermalResult.put("status", "Грешка: " + e.getMessage());
                thermalResult.put("name", thermalPrinterName);
                thermalResult.put("connection", thermalConnection);
            }
        } else {
            thermalResult.put("connected", false);
            thermalResult.put("status", "Оневозможен");
        }
        results.put("thermal", thermalResult);
        
        return results;
    }
    
    /**
     * Get current printer configuration
     */
    public PrinterConfiguration getPrinterConfiguration() {
        PrinterConfiguration config = new PrinterConfiguration();
        
        // Fiscal printer config
        config.setFiscalEnabled(fiscalPrinterEnabled);
        config.setFiscalPort(fiscalPrinterPort);
        config.setFiscalType(fiscalPrinterType);
        
        // Thermal printer config
        config.setThermalEnabled(thermalPrinterEnabled);
        config.setThermalName(thermalPrinterName);
        config.setThermalConnection(thermalConnection);
        config.setThermalIP(thermalPrinterIP);
        config.setThermalPort(thermalPrinterPort);
        
        return config;
    }
    
    /**
     * Update printer configuration
     */
    public void updatePrinterConfiguration(PrinterConfiguration config) {
        // Update fiscal printer settings
        this.fiscalPrinterEnabled = config.isFiscalEnabled();
        this.fiscalPrinterPort = config.getFiscalPort();
        this.fiscalPrinterType = config.getFiscalType();
        
        // Update thermal printer settings
        this.thermalPrinterEnabled = config.isThermalEnabled();
        this.thermalPrinterName = config.getThermalName();
        this.thermalConnection = config.getThermalConnection();
        this.thermalPrinterIP = config.getThermalIP();
        this.thermalPrinterPort = config.getThermalPort();
        
        // Reinitialize printers with new settings
        initializePrinters();
        
        logger.info("Printer configuration updated");
    }
    
    /**
     * FIXED: Get available COM ports and LPT ports for fiscal printers
     */
    public List<String> getAvailableComPorts() {
        List<String> ports = new ArrayList<>();
        try {
            if (fiscalPrinter != null) {
                ports = fiscalPrinter.getAvailablePorts();
            } else {
                // Fallback - add common ports
                // COM ports
                for (int i = 1; i <= 16; i++) {
                    ports.add("COM" + i);
                }
                // LPT ports
                for (int i = 1; i <= 3; i++) {
                    ports.add("LPT" + i);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting ports", e);
            // Minimal fallback
            ports.add("COM1");
            ports.add("COM2");
            ports.add("LPT1");
            ports.add("LPT2");
        }
        return ports;
    }
    
    /**
     * Get available thermal printers
     */
    public List<String> getAvailableThermalPrinters() {
        List<String> printers = new ArrayList<>();
        try {
            if (thermalPrinter != null) {
                printers = thermalPrinter.getAvailablePrinters();
            } else {
                // Fallback - add common thermal printer names
                printers.add("Epson TM-T20II");
                printers.add("Epson TM-T88V");
                printers.add("Epson TM-T82");
                printers.add("Generic ESC/POS Printer");
            }
        } catch (Exception e) {
            logger.error("Error getting thermal printers", e);
        }
        return printers;
    }
    
    public boolean validateAdminCode(String code) {
        return ADMIN_CODE.equals(code);
    }
    
    // FIXED: Fallback methods for when printers are not available
    // Now correctly shows only NEW items
    private void printSimulatedTickets(Order order) {
        try {
            // FIXED: Separate NEW items by print destination
            List<OrderItem> newKitchenItems = order.getItems().stream()
                .filter(item -> {
                    boolean isKitchen = item.getMenuItem().getPrintDestination() == MenuItem.PrintDestination.КУЈНА;
                    int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                    return isKitchen && pendingQty > 0;
                })
                .toList();
            
            List<OrderItem> newBarItems = order.getItems().stream()
                .filter(item -> {
                    boolean isBar = item.getMenuItem().getPrintDestination() == MenuItem.PrintDestination.БАР;
                    int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
                    return isBar && pendingQty > 0;
                })
                .toList();
            
            // Print kitchen ticket if there are NEW kitchen items
            if (!newKitchenItems.isEmpty()) {
                printSimulatedKitchenTicket(order, newKitchenItems);
            }
            
            // Print bar ticket if there are NEW bar items
            if (!newBarItems.isEmpty()) {
                printSimulatedBarTicket(order, newBarItems);
            }
            
            // Log if no new items
            if (newKitchenItems.isEmpty() && newBarItems.isEmpty()) {
                System.out.println("=== НЕ ДОСТАПЕН ПРИНТЕР ===");
                System.out.println("Нема нови производи за печатење во нарачка " + order.getId());
                System.out.println("===========================");
            }
            
        } catch (Exception e) {
            logger.error("Error printing simulated tickets", e);
        }
    }
    
    private void printSimulatedKitchenTicket(Order order, List<OrderItem> newItems) {
        String ticketContent = formatKitchenTicket(order, newItems);
        System.out.println("=== КУЈНА БИЛЕТ (СИМУЛИРАН) ===");
        System.out.println(ticketContent);
        System.out.println("===============================");
    }
    
    private void printSimulatedBarTicket(Order order, List<OrderItem> newItems) {
        String ticketContent = formatBarTicket(order, newItems);
        System.out.println("=== БАР БИЛЕТ (СИМУЛИРАН) ===");
        System.out.println(ticketContent);
        System.out.println("=============================");
    }
    
    private void printSimulatedReceipt(Order order) {
        String receiptContent = formatReceipt(order);
        System.out.println("=== СМЕТКА (СИМУЛИРАНА) ===");
        System.out.println(receiptContent);
        System.out.println("===========================");
    }
    
    // FIXED: Format methods now correctly show only NEW items and their pending quantities
    private String formatKitchenTicket(Order order, List<OrderItem> newItems) {
        StringBuilder ticket = new StringBuilder();
        ticket.append("========== КУЈНА ==========\n");
        ticket.append("Маса: ").append(getOrderDisplayName(order)).append("\n");
        ticket.append("Време: ").append(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt())).append("\n");
        ticket.append("===========================\n\n");
        
        for (OrderItem item : newItems) {
            int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
            
            ticket.append(pendingQty).append("x ")  // Show only pending quantity
                  .append(item.getMenuItem().getName()).append("\n");
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                ticket.append("   Забелешка: ").append(item.getNotes()).append("\n");
            }
            ticket.append("\n");
        }
        
        ticket.append("===========================\n");
        ticket.append("НОВИ ПРОИЗВОДИ: ").append(newItems.size()).append("\n");
        return ticket.toString();
    }
    
    private String formatBarTicket(Order order, List<OrderItem> newItems) {
        StringBuilder ticket = new StringBuilder();
        ticket.append("=========== БАР ===========\n");
        ticket.append("Маса: ").append(getOrderDisplayName(order)).append("\n");
        ticket.append("Време: ").append(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt())).append("\n");
        ticket.append("===========================\n\n");
        
        for (OrderItem item : newItems) {
            int pendingQty = item.getQuantity() - (item.getSentQuantity() != null ? item.getSentQuantity() : 0);
            
            ticket.append(pendingQty).append("x ")  // Show only pending quantity
                  .append(item.getMenuItem().getName()).append("\n");
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                ticket.append("   Забелешка: ").append(item.getNotes()).append("\n");
            }
            ticket.append("\n");
        }
        
        ticket.append("===========================\n");
        ticket.append("НОВИ ПРОИЗВОДИ: ").append(newItems.size()).append("\n");
        return ticket.toString();
    }
    
    private String formatReceipt(Order order) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("======= РЕСТОРАН POS =======\n");
        receipt.append("Маса: ").append(getOrderDisplayName(order)).append("\n");
        receipt.append("Датум: ").append(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(order.getCreatedAt())).append("\n");
        receipt.append("============================\n\n");
        
        for (OrderItem item : order.getItems()) {
            receipt.append(String.format("%dx %-15s %8.2f ден\n", 
                item.getQuantity(), 
                item.getMenuItem().getName(), 
                item.getTotalPrice()));
        }
        
        receipt.append("\n============================\n");
        receipt.append(String.format("ВКУПНО: %20.2f ден\n", order.getTotalAmount()));
        receipt.append("============================\n\n");
        receipt.append("    Ви благодариме!\n");
        receipt.append("============================\n");
        
        return receipt.toString();
    }
    
    /**
     * FIXED: Helper method to get order display name
     */
    private String getOrderDisplayName(Order order) {
        if (order.getTableNumber() >= 1000) {
            return "Понеси #" + (order.getTableNumber() - 1000 + 1);
        } else {
            return String.valueOf(order.getTableNumber());
        }
    }
}