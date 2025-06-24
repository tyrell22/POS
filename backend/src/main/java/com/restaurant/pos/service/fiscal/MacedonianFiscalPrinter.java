package com.restaurant.pos.service.fiscal;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.entity.OrderItem;
import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Macedonian Fiscal Printer Implementation
 * FIXED: Support for both COM ports and LPT parallel ports
 * Based on the protocol used by Synergy PF-500 and Expert SX fiscal printers
 */
public class MacedonianFiscalPrinter {

    private static final Logger logger = LoggerFactory.getLogger(MacedonianFiscalPrinter.class);

    // Protocol constants
    private static final byte STX = 0x02;  // Start of text
    private static final byte ETX = 0x03;  // End of text
    private static final byte ACK = 0x06;  // Acknowledge
    private static final byte NAK = 0x15;  // Negative acknowledge
    private static final byte ENQ = 0x05;  // Enquiry

    // Command constants
    private static final String CMD_OPEN_RECEIPT = "30";
    private static final String CMD_SALE = "31";
    private static final String CMD_PAYMENT = "53";
    private static final String CMD_CLOSE_RECEIPT = "56";
    private static final String CMD_STATUS = "74";
    private static final String CMD_PRINT_COPY = "6D";

    // Tax groups
    private static final String TAX_GROUP_A = "А"; // 18% VAT
    private static final String TAX_GROUP_B = "Б"; // 5% VAT
    private static final String TAX_GROUP_C = "В"; // 0% VAT

    private String portName;
    private String printerType;
    private SerialPort serialPort;
    private OutputStream parallelPort; // FIXED: Added support for parallel port
    private boolean isParallelPort; // FIXED: Track connection type
    private int timeout = 5000; // 5 seconds timeout

    public MacedonianFiscalPrinter(String portName, String printerType) {
        this.portName = portName;
        this.printerType = printerType;
        this.isParallelPort = portName.toUpperCase().startsWith("LPT"); // FIXED: Detect LPT ports
        logger.info("Initialized Macedonian fiscal printer: {} on port {} ({})", 
            printerType, portName, isParallelPort ? "Parallel" : "Serial");
    }

    /**
     * FIXED: Test connection to the fiscal printer (both serial and parallel)
     */
    public boolean testConnection() {
        try {
            if (isParallelPort) {
                return testParallelConnection();
            } else {
                return testSerialConnection();
            }
        } catch (Exception e) {
            logger.error("Error testing fiscal printer connection", e);
            return false;
        }
    }

    /**
     * FIXED: Test parallel port connection
     */
    private boolean testParallelConnection() {
        try {
            // Try to open the parallel port device
            String devicePath = getParallelDevicePath(portName);
            File device = new File(devicePath);
            
            if (!device.exists()) {
                logger.warn("Parallel port device {} does not exist", devicePath);
                return false;
            }
            
            // Try to write a simple status command
            try (FileOutputStream fos = new FileOutputStream(device)) {
                // Send a simple status request
                byte[] statusCmd = buildSimpleCommand(CMD_STATUS);
                fos.write(statusCmd);
                fos.flush();
                logger.info("Successfully sent test command to parallel port {}", portName);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error testing parallel connection to {}", portName, e);
            return false;
        }
    }

    /**
     * FIXED: Test serial port connection
     */
    private boolean testSerialConnection() {
        try {
            if (openSerialConnection()) {
                // Send status request
                String response = sendSerialCommand(CMD_STATUS, "");
                closeConnection();
                return response != null && !response.isEmpty();
            }
            return false;
        } catch (Exception e) {
            logger.error("Error testing serial connection to {}", portName, e);
            return false;
        }
    }

    /**
     * Print fiscal receipt
     */
    public void printFiscalReceipt(Order order) throws Exception {
        logger.info("Starting fiscal receipt printing for order {}", order.getId());

        if (!openConnection()) {
            throw new Exception("Cannot connect to fiscal printer on port " + portName);
        }

        try {
            if (isParallelPort) {
                printFiscalReceiptParallel(order);
            } else {
                printFiscalReceiptSerial(order);
            }
            logger.info("Fiscal receipt printed successfully for order {}", order.getId());
        } catch (Exception e) {
            logger.error("Error printing fiscal receipt for order {}", order.getId(), e);
            throw e;
        } finally {
            closeConnection();
        }
    }

    /**
     * FIXED: Print fiscal receipt via parallel port
     */
    private void printFiscalReceiptParallel(Order order) throws Exception {
        logger.info("Printing fiscal receipt via parallel port for order {}", order.getId());
        
        try {
            // 1. Open fiscal receipt
            byte[] openCmd = buildFiscalCommand(CMD_OPEN_RECEIPT, "1,Касиер," + getCurrentOperatorPassword());
            parallelPort.write(openCmd);
            parallelPort.flush();
            Thread.sleep(500); // Wait for command processing

            // 2. Add items to receipt
            for (OrderItem item : order.getItems()) {
                String itemName = sanitizeText(item.getMenuItem().getName());
                BigDecimal unitPrice = item.getUnitPrice();
                int quantity = item.getQuantity();
                String taxGroup = determineTaxGroup(item.getMenuItem(), order);
                
                String saleData = String.format("%s,%s,%.2f,%d,%s",
                    itemName, unitPrice.toString(), unitPrice.doubleValue(), quantity, taxGroup);

                byte[] saleCmd = buildFiscalCommand(CMD_SALE, saleData);
                parallelPort.write(saleCmd);
                parallelPort.flush();
                Thread.sleep(200); // Wait between items
            }

            // 3. Add payment
            String paymentData = String.format("%.2f,P", order.getTotalAmount().doubleValue());
            byte[] paymentCmd = buildFiscalCommand(CMD_PAYMENT, paymentData);
            parallelPort.write(paymentCmd);
            parallelPort.flush();
            Thread.sleep(500);

            // 4. Close fiscal receipt
            byte[] closeCmd = buildFiscalCommand(CMD_CLOSE_RECEIPT, "");
            parallelPort.write(closeCmd);
            parallelPort.flush();
            Thread.sleep(1000); // Wait for receipt to finish printing

        } catch (Exception e) {
            logger.error("Error in parallel fiscal printing", e);
            throw e;
        }
    }

    /**
     * FIXED: Print fiscal receipt via serial port (existing logic)
     */
    private void printFiscalReceiptSerial(Order order) throws Exception {
        // 1. Open fiscal receipt
        String response = sendSerialCommand(CMD_OPEN_RECEIPT, "1,Касиер," + getCurrentOperatorPassword());
        if (!isSuccessResponse(response)) {
            throw new Exception("Failed to open fiscal receipt: " + response);
        }

        // 2. Add items to receipt
        for (OrderItem item : order.getItems()) {
            String itemName = sanitizeText(item.getMenuItem().getName());
            BigDecimal unitPrice = item.getUnitPrice();
            int quantity = item.getQuantity();
            String taxGroup = determineTaxGroup(item.getMenuItem(), order);
            
            String saleData = String.format("%s,%s,%.2f,%d,%s",
                itemName, unitPrice.toString(), unitPrice.doubleValue(), quantity, taxGroup);

            response = sendSerialCommand(CMD_SALE, saleData);
            if (!isSuccessResponse(response)) {
                logger.warn("Failed to add item to fiscal receipt: {} - {}", itemName, response);
            }
        }

        // 3. Add payment
        String paymentData = String.format("%.2f,P", order.getTotalAmount().doubleValue());
        response = sendSerialCommand(CMD_PAYMENT, paymentData);
        if (!isSuccessResponse(response)) {
            throw new Exception("Failed to add payment: " + response);
        }

        // 4. Close fiscal receipt
        response = sendSerialCommand(CMD_CLOSE_RECEIPT, "");
        if (!isSuccessResponse(response)) {
            throw new Exception("Failed to close fiscal receipt: " + response);
        }
    }

    /**
     * FIXED: Get list of available ports (both COM and LPT)
     */
    public List<String> getAvailablePorts() {
        List<String> ports = new ArrayList<>();
        
        // Add COM ports
        SerialPort[] availablePorts = SerialPort.getCommPorts();
        for (SerialPort port : availablePorts) {
            ports.add(port.getSystemPortName());
        }

        // Add common COM ports even if not detected
        for (int i = 1; i <= 16; i++) {
            String comPort = "COM" + i;
            if (!ports.contains(comPort)) {
                ports.add(comPort);
            }
        }

        // FIXED: Add LPT (parallel) ports
        for (int i = 1; i <= 3; i++) {
            String lptPort = "LPT" + i;
            ports.add(lptPort);
            
            // Also check if the device file exists on Windows
            String devicePath = getParallelDevicePath(lptPort);
            File device = new File(devicePath);
            if (device.exists()) {
                logger.info("Found parallel port device: {}", devicePath);
            }
        }

        logger.info("Available ports: {}", ports);
        return ports;
    }

    /**
     * FIXED: Open connection to fiscal printer (both serial and parallel)
     */
    private boolean openConnection() {
        try {
            if (isParallelPort) {
                return openParallelConnection();
            } else {
                return openSerialConnection();
            }
        } catch (Exception e) {
            logger.error("Error opening connection to fiscal printer", e);
            return false;
        }
    }

    /**
     * FIXED: Open parallel port connection
     */
    private boolean openParallelConnection() {
        try {
            String devicePath = getParallelDevicePath(portName);
            File device = new File(devicePath);
            
            if (!device.exists()) {
                logger.error("Parallel port device {} does not exist", devicePath);
                return false;
            }
            
            parallelPort = new FileOutputStream(device);
            logger.debug("Opened parallel connection to fiscal printer on {}", portName);
            return true;
        } catch (Exception e) {
            logger.error("Error opening parallel connection to fiscal printer", e);
            return false;
        }
    }

    /**
     * FIXED: Open serial port connection (existing logic)
     */
    private boolean openSerialConnection() {
        try {
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(9600);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, 0);

            if (serialPort.openPort()) {
                Thread.sleep(100); // Wait for port to stabilize
                logger.debug("Opened serial connection to fiscal printer on {}", portName);
                return true;
            } else {
                logger.error("Failed to open port {}", portName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error opening serial connection to fiscal printer", e);
            return false;
        }
    }

    /**
     * FIXED: Close connection to fiscal printer (both serial and parallel)
     */
    private void closeConnection() {
        try {
            if (parallelPort != null) {
                parallelPort.close();
                parallelPort = null;
                logger.debug("Closed parallel connection to fiscal printer");
            }
            
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                logger.debug("Closed serial connection to fiscal printer");
            }
        } catch (Exception e) {
            logger.error("Error closing connection to fiscal printer", e);
        }
    }

    /**
     * FIXED: Get parallel port device path for Windows
     */
    private String getParallelDevicePath(String portName) {
        // On Windows, parallel ports can be accessed as \\.\LPT1, \\.\LPT2, etc.
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return "\\\\.\\" + portName.toUpperCase();
        } else {
            // On Linux, might be /dev/lp0, /dev/lp1, etc.
            int portNum = Integer.parseInt(portName.substring(3)) - 1; // LPT1 -> 0
            return "/dev/lp" + portNum;
        }
    }

    /**
     * FIXED: Build simple fiscal command for parallel port
     */
    private byte[] buildSimpleCommand(String command) {
        String packet = command;
        return packet.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * FIXED: Build fiscal command with proper framing for parallel port
     */
    private byte[] buildFiscalCommand(String command, String data) {
        String packet = command;
        if (!data.isEmpty()) {
            packet += "," + data;
        }
        
        // Calculate checksum
        int checksum = calculateChecksum(packet);
        String fullPacket = packet + String.format("%04X", checksum);

        // Add protocol framing
        return buildPacket(fullPacket);
    }

    /**
     * Send command to fiscal printer (serial only)
     */
    private String sendSerialCommand(String command, String data) throws Exception {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new Exception("Serial port is not open");
        }

        // Build command packet
        String packet = command;
        if (!data.isEmpty()) {
            packet += "," + data;
        }

        // Calculate checksum
        int checksum = calculateChecksum(packet);
        String fullPacket = packet + String.format("%04X", checksum);

        // Add protocol framing
        byte[] commandBytes = buildPacket(fullPacket);

        logger.debug("Sending serial command: {}", fullPacket);

        // Send command
        OutputStream outputStream = serialPort.getOutputStream();
        outputStream.write(commandBytes);
        outputStream.flush();

        // Wait for response
        String response = readResponse();
        logger.debug("Received serial response: {}", response);

        return response;
    }

    /**
     * Build packet with protocol framing
     */
    private byte[] buildPacket(String data) {
        List<Byte> packet = new ArrayList<>();
        packet.add(STX);

        for (byte b : data.getBytes(StandardCharsets.ISO_8859_1)) {
            packet.add(b);
        }

        packet.add(ETX);

        // Convert to byte array
        byte[] result = new byte[packet.size()];
        for (int i = 0; i < packet.size(); i++) {
            result[i] = packet.get(i);
        }

        return result;
    }

    /**
     * Read response from fiscal printer (serial only)
     */
    private String readResponse() throws Exception {
        InputStream inputStream = serialPort.getInputStream();
        List<Byte> responseBytes = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        boolean stxReceived = false;

        while (System.currentTimeMillis() - startTime < timeout) {
            if (inputStream.available() > 0) {
                int byteRead = inputStream.read();

                if (byteRead == STX) {
                    stxReceived = true;
                    responseBytes.clear();
                    continue;
                }

                if (byteRead == ETX && stxReceived) {
                    break;
                }

                if (stxReceived) {
                    responseBytes.add((byte) byteRead);
                }
            }

            Thread.sleep(10);
        }

        if (responseBytes.isEmpty()) {
            throw new Exception("No response received from fiscal printer");
        }

        // Convert to string
        byte[] responseArray = new byte[responseBytes.size()];
        for (int i = 0; i < responseBytes.size(); i++) {
            responseArray[i] = responseBytes.get(i);
        }

        return new String(responseArray, StandardCharsets.ISO_8859_1);
    }

    /**
     * Calculate checksum for command
     */
    private int calculateChecksum(String data) {
        int sum = 0;
        for (byte b : data.getBytes(StandardCharsets.ISO_8859_1)) {
            sum += b & 0xFF;
        }
        return sum & 0xFFFF;
    }

    /**
     * Check if response indicates success
     */
    private boolean isSuccessResponse(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        // Parse response status
        // Format is usually: command,status,data
        String[] parts = response.split(",");
        if (parts.length >= 2) {
            try {
                int status = Integer.parseInt(parts[1]);
                return status == 0; // 0 means success
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Determine tax group for menu item based on order type
     * Takeout orders: 5% VAT (Tax Group B)
     * Dine-in orders: 18% VAT (Tax Group A)
     */
    private String determineTaxGroup(com.restaurant.pos.entity.MenuItem menuItem, Order order) {
        // Check if this is a takeout order (table numbers 1000+ are takeout)
        boolean isTakeout = order.getTableNumber() >= 1000;

        if (isTakeout) {
            // Takeout orders use 5% VAT (Tax Group B) for all items
            logger.debug("Applying 5% VAT (Tax Group B) for takeout order - Table: {}, Item: {}",
                order.getTableNumber(), menuItem.getName());
            return TAX_GROUP_B;
        } else {
            // Dine-in orders use 18% VAT (Tax Group A) for all items
            logger.debug("Applying 18% VAT (Tax Group A) for dine-in order - Table: {}, Item: {}",
                order.getTableNumber(), menuItem.getName());
            return TAX_GROUP_A;
        }
    }

    /**
     * Sanitize text for fiscal printer
     */
    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        // Remove special characters that might cause issues
        String sanitized = text.replaceAll("[^\\p{L}\\p{N}\\s\\-\\.]", "");

        // Limit length to what fiscal printer can handle
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }

        return sanitized;
    }

    /**
     * Get current operator password (configurable)
     */
    private String getCurrentOperatorPassword() {
        // This should be configurable - using default for now
        return "1";
    }
}