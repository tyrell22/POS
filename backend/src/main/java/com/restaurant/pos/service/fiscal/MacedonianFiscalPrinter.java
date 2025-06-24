package com.restaurant.pos.service.fiscal;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.entity.OrderItem;
import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Macedonian Fiscal Printer Implementation
 * Based on the protocol used by Synergy PF-500 and Expert SX fiscal printers
 * Supports the standard Macedonian fiscal printer protocol
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
    private int timeout = 5000; // 5 seconds timeout

    public MacedonianFiscalPrinter(String portName, String printerType) {
        this.portName = portName;
        this.printerType = printerType;
        logger.info("Initialized Macedonian fiscal printer: {} on port {}", printerType, portName);
    }

    /**
     * Test connection to the fiscal printer
     */
    public boolean testConnection() {
        try {
            if (openConnection()) {
                // Send status request
                String response = sendCommand(CMD_STATUS, "");
                closeConnection();
                return response != null && !response.isEmpty();
            }
            return false;
        } catch (Exception e) {
            logger.error("Error testing fiscal printer connection", e);
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
            // 1. Open fiscal receipt
            String response = sendCommand(CMD_OPEN_RECEIPT, "1,Касиер," + getCurrentOperatorPassword());
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

            response = sendCommand(CMD_SALE, saleData);
            if (!isSuccessResponse(response)) {
                logger.warn("Failed to add item to fiscal receipt: {} - {}", itemName, response);
            }
        }

            // 3. Add payment
            String paymentData = String.format("%.2f,P", order.getTotalAmount().doubleValue());
        response = sendCommand(CMD_PAYMENT, paymentData);
        if (!isSuccessResponse(response)) {
            throw new Exception("Failed to add payment: " + response);
        }

        // 4. Close fiscal receipt
        response = sendCommand(CMD_CLOSE_RECEIPT, "");
        if (!isSuccessResponse(response)) {
            throw new Exception("Failed to close fiscal receipt: " + response);
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
     * Get list of available COM ports
     */
    public List < String > getAvailablePorts() {
    List < String > ports = new ArrayList <> ();
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

    return ports;
}

    /**
     * Open connection to fiscal printer
     */
    private boolean openConnection() {
    try {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, 0);

        if (serialPort.openPort()) {
            Thread.sleep(100); // Wait for port to stabilize
            logger.debug("Opened connection to fiscal printer on {}", portName);
            return true;
        } else {
            logger.error("Failed to open port {}", portName);
            return false;
        }
    } catch (Exception e) {
        logger.error("Error opening connection to fiscal printer", e);
        return false;
    }
}

    /**
     * Close connection to fiscal printer
     */
    private void closeConnection() {
    if (serialPort != null && serialPort.isOpen()) {
        serialPort.closePort();
        logger.debug("Closed connection to fiscal printer");
    }
}

    /**
     * Send command to fiscal printer
     */
    private String sendCommand(String command, String data) throws Exception {
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

    logger.debug("Sending command: {}", fullPacket);

        // Send command
        OutputStream outputStream = serialPort.getOutputStream();
    outputStream.write(commandBytes);
    outputStream.flush();

        // Wait for response
        String response = readResponse();
    logger.debug("Received response: {}", response);

    return response;
}

    /**
     * Build packet with protocol framing
     */
    private byte[] buildPacket(String data) {
    List < Byte > packet = new ArrayList <> ();
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
     * Read response from fiscal printer
     */
    private String readResponse() throws Exception {
        InputStream inputStream = serialPort.getInputStream();
    List < Byte > responseBytes = new ArrayList <> ();
        
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

    // Note: If you need different tax rates based on item category in the future,
    // you can add additional logic here:
    /*
    switch (menuItem.getCategory()) {
        case ХРАНА:
            return isTakeout ? TAX_GROUP_B : TAX_GROUP_A;
        case ПИЈАЛОЦИ:
            return isTakeout ? TAX_GROUP_B : TAX_GROUP_A;
        case АЛКОХОЛ:
            return TAX_GROUP_A; // Alcohol might always be 18% regardless of takeout
        case ДЕСЕРТИ:
            return isTakeout ? TAX_GROUP_B : TAX_GROUP_A;
        default:
            return isTakeout ? TAX_GROUP_B : TAX_GROUP_A;
    }
    */
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