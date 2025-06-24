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
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Macedonian Fiscal Printer Implementation
 * FIXED: Better Windows LPT port support with multiple connection methods
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
    private OutputStream parallelPort;
    private boolean isParallelPort;
    private int timeout = 5000; // 5 seconds timeout

    public MacedonianFiscalPrinter(String portName, String printerType) {
        this.portName = portName;
        this.printerType = printerType;
        this.isParallelPort = portName.toUpperCase().startsWith("LPT");
        logger.info("Initialized Macedonian fiscal printer: {} on port {} ({})", 
            printerType, portName, isParallelPort ? "Parallel" : "Serial");
    }

    /**
     * ENHANCED: Test connection with multiple methods for LPT ports
     */
    public boolean testConnection() {
        try {
            if (isParallelPort) {
                return testParallelConnectionMultipleMethods();
            } else {
                return testSerialConnection();
            }
        } catch (Exception e) {
            logger.error("Error testing fiscal printer connection", e);
            return false;
        }
    }

    /**
     * ENHANCED: Try multiple methods to connect to LPT port
     */
    private boolean testParallelConnectionMultipleMethods() {
        logger.info("Testing parallel port {} with multiple methods", portName);
        
        // Method 1: Try Windows device path
        if (testWindowsDevicePath()) {
            logger.info("✅ Windows device path method works for {}", portName);
            return true;
        }
        
        // Method 2: Try direct file access
        if (testDirectFileAccess()) {
            logger.info("✅ Direct file access method works for {}", portName);
            return true;
        }
        
        // Method 3: Try PRN device (Windows printer port)
        if (testPrnDevice()) {
            logger.info("✅ PRN device method works for {}", portName);
            return true;
        }
        
        logger.warn("❌ All parallel port connection methods failed for {}", portName);
        return false;
    }

    /**
     * Method 1: Windows device path \\.\LPT1
     */
    private boolean testWindowsDevicePath() {
        try {
            String devicePath = "\\\\.\\" + portName.toUpperCase();
            logger.debug("Testing Windows device path: {}", devicePath);
            
            File device = new File(devicePath);
            if (!device.exists()) {
                logger.debug("Device file {} does not exist", devicePath);
                return false;
            }
            
            try (FileOutputStream fos = new FileOutputStream(device)) {
                // Try to write a simple command
                fos.write("TEST".getBytes());
                fos.flush();
                logger.debug("Successfully wrote to {}", devicePath);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Windows device path test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Method 2: Direct file access
     */
    private boolean testDirectFileAccess() {
        try {
            logger.debug("Testing direct file access to {}", portName);
            
            try (RandomAccessFile raf = new RandomAccessFile(portName, "rw")) {
                raf.writeBytes("TEST");
                logger.debug("Successfully wrote to {} via RandomAccessFile", portName);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Direct file access test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Method 3: PRN device (Windows printer device)
     */
    private boolean testPrnDevice() {
        try {
            String prnPath = "PRN";
            logger.debug("Testing PRN device access");
            
            try (FileOutputStream fos = new FileOutputStream(prnPath)) {
                fos.write("TEST".getBytes());
                fos.flush();
                logger.debug("Successfully wrote to PRN device");
                return true;
            }
        } catch (Exception e) {
            logger.debug("PRN device test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ENHANCED: Open parallel connection with best available method
     */
    private boolean openParallelConnection() {
        logger.info("Opening parallel connection to {}", portName);
        
        // Try the same methods as test, but keep the connection open
        
        // Method 1: Windows device path
        try {
            String devicePath = "\\\\.\\" + portName.toUpperCase();
            File device = new File(devicePath);
            if (device.exists()) {
                parallelPort = new FileOutputStream(device);
                logger.info("✅ Connected via Windows device path: {}", devicePath);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Windows device path connection failed: {}", e.getMessage());
        }
        
        // Method 2: Direct file access
        try {
            parallelPort = new FileOutputStream(portName);
            logger.info("✅ Connected via direct file access: {}", portName);
            return true;
        } catch (Exception e) {
            logger.debug("Direct file access connection failed: {}", e.getMessage());
        }
        
        // Method 3: PRN device
        try {
            parallelPort = new FileOutputStream("PRN");
            logger.info("✅ Connected via PRN device");
            return true;
        } catch (Exception e) {
            logger.debug("PRN device connection failed: {}", e.getMessage());
        }
        
        logger.error("❌ All parallel connection methods failed for {}", portName);
        return false;
    }

    /**
     * Test serial port connection
     */
    private boolean testSerialConnection() {
        try {
            if (openSerialConnection()) {
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
     * Print fiscal receipt via parallel port
     */
    private void printFiscalReceiptParallel(Order order) throws Exception {
        logger.info("Printing fiscal receipt via parallel port for order {}", order.getId());
        
        try {
            // 1. Open fiscal receipt
            byte[] openCmd = buildFiscalCommand(CMD_OPEN_RECEIPT, "1,Касиер,1");
            parallelPort.write(openCmd);
            parallelPort.flush();
            Thread.sleep(500);

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
                Thread.sleep(200);
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
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.error("Error in parallel fiscal printing", e);
            throw e;
        }
    }

    /**
     * Print fiscal receipt via serial port
     */
    private void printFiscalReceiptSerial(Order order) throws Exception {
        // 1. Open fiscal receipt
        String response = sendSerialCommand(CMD_OPEN_RECEIPT, "1,Касиер,1");
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
     * ENHANCED: Get list of available ports with better Windows LPT detection
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

        // ENHANCED: Test and add LPT ports that actually work
        for (int i = 1; i <= 3; i++) {
            String lptPort = "LPT" + i;
            
            // Test if this LPT port is available
            try {
                MacedonianFiscalPrinter testPrinter = new MacedonianFiscalPrinter(lptPort, "TEST");
                if (testPrinter.testParallelConnectionMultipleMethods()) {
                    ports.add(lptPort + " ✅"); // Mark as working
                    logger.info("LPT port {} is available and working", lptPort);
                } else {
                    ports.add(lptPort + " ❌"); // Mark as not working
                    logger.debug("LPT port {} is not available", lptPort);
                }
            } catch (Exception e) {
                ports.add(lptPort + " ❌");
                logger.debug("LPT port {} test failed: {}", lptPort, e.getMessage());
            }
        }

        logger.info("Available ports: {}", ports);
        return ports;
    }

    /**
     * Open connection (enhanced)
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
     * Open serial connection
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
                Thread.sleep(100);
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
     * Close connection
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

    // ... (rest of the methods remain the same as before)
    
    private byte[] buildFiscalCommand(String command, String data) {
        String packet = command;
        if (!data.isEmpty()) {
            packet += "," + data;
        }
        
        int checksum = calculateChecksum(packet);
        String fullPacket = packet + String.format("%04X", checksum);
        return buildPacket(fullPacket);
    }
    
    private String sendSerialCommand(String command, String data) throws Exception {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new Exception("Serial port is not open");
        }

        String packet = command;
        if (!data.isEmpty()) {
            packet += "," + data;
        }

        int checksum = calculateChecksum(packet);
        String fullPacket = packet + String.format("%04X", checksum);
        byte[] commandBytes = buildPacket(fullPacket);

        logger.debug("Sending serial command: {}", fullPacket);

        OutputStream outputStream = serialPort.getOutputStream();
        outputStream.write(commandBytes);
        outputStream.flush();

        String response = readResponse();
        logger.debug("Received serial response: {}", response);
        return response;
    }

    private byte[] buildPacket(String data) {
        List<Byte> packet = new ArrayList<>();
        packet.add(STX);
        for (byte b : data.getBytes(StandardCharsets.ISO_8859_1)) {
            packet.add(b);
        }
        packet.add(ETX);

        byte[] result = new byte[packet.size()];
        for (int i = 0; i < packet.size(); i++) {
            result[i] = packet.get(i);
        }
        return result;
    }

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

        byte[] responseArray = new byte[responseBytes.size()];
        for (int i = 0; i < responseBytes.size(); i++) {
            responseArray[i] = responseBytes.get(i);
        }
        return new String(responseArray, StandardCharsets.ISO_8859_1);
    }

    private int calculateChecksum(String data) {
        int sum = 0;
        for (byte b : data.getBytes(StandardCharsets.ISO_8859_1)) {
            sum += b & 0xFF;
        }
        return sum & 0xFFFF;
    }

    private boolean isSuccessResponse(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        String[] parts = response.split(",");
        if (parts.length >= 2) {
            try {
                int status = Integer.parseInt(parts[1]);
                return status == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private String determineTaxGroup(com.restaurant.pos.entity.MenuItem menuItem, Order order) {
        boolean isTakeout = order.getTableNumber() >= 1000;

        if (isTakeout) {
            logger.debug("Applying 5% VAT (Tax Group B) for takeout order - Table: {}, Item: {}",
                order.getTableNumber(), menuItem.getName());
            return TAX_GROUP_B;
        } else {
            logger.debug("Applying 18% VAT (Tax Group A) for dine-in order - Table: {}, Item: {}",
                order.getTableNumber(), menuItem.getName());
            return TAX_GROUP_A;
        }
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text.replaceAll("[^\\p{L}\\p{N}\\s\\-\\.]", "");

        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }
        return sanitized;
    }
}
