package com.restaurant.pos.controller;

import com.restaurant.pos.service.PrinterService;
import com.restaurant.pos.dto.PrinterConfiguration;
import com.restaurant.pos.dto.AdminLoginRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/printer")
@CrossOrigin(origins = "*")
public class PrinterController {

    private static final Logger logger = LoggerFactory.getLogger(PrinterController.class);

    @Autowired
    private PrinterService printerService;

    /**
     * Get current printer configuration
     */
    @GetMapping("/config")
    public ResponseEntity<?> getPrinterConfiguration(@RequestHeader(value = "Admin-Code", required = false) String adminCode) {
        try {
            // Validate admin access
            if (adminCode == null || !printerService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неавторизиран пристап"));
            }
            
            PrinterConfiguration config = printerService.getPrinterConfiguration();
            return ResponseEntity.ok(config);

        } catch (Exception e) {
            logger.error("Error getting printer configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на конфигурацијата"));
        }
    }

    /**
     * Update printer configuration
     */
    @PutMapping("/config")
    public ResponseEntity<?> updatePrinterConfiguration(
        @Valid @RequestBody PrinterConfiguration config,
        @RequestHeader(value = "Admin-Code", required = false) String adminCode) {
        try {
            // Validate admin access
            if (adminCode == null || !printerService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неавторизиран пристап"));
            }

            printerService.updatePrinterConfiguration(config);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Конфигурацијата е ажурирана успешно"
            ));

        } catch (Exception e) {
            logger.error("Error updating printer configuration", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Грешка при ажурирање на конфигурацијата: " + e.getMessage()));
        }
    }

    /**
     * Test printer connections
     */
    @PostMapping("/test")
    public ResponseEntity<?> testPrinterConnections(@RequestHeader(value = "Admin-Code", required = false) String adminCode) {
        try {
            // Validate admin access
            if (adminCode == null || !printerService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неавторизиран пристап"));
            }

            Map < String, Object > testResults = printerService.testPrinterConnections();
            return ResponseEntity.ok(testResults);

        } catch (Exception e) {
            logger.error("Error testing printer connections", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при тестирање на принтерите"));
        }
    }

    /**
     * Get available COM ports for fiscal printers
     */
    @GetMapping("/ports")
    public ResponseEntity<?> getAvailableComPorts(@RequestHeader(value = "Admin-Code", required = false) String adminCode) {
        try {
            // Validate admin access
            if (adminCode == null || !printerService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неавторизиран пристап"));
            }

            List < String > ports = printerService.getAvailableComPorts();
            return ResponseEntity.ok(Map.of("ports", ports));

        } catch (Exception e) {
            logger.error("Error getting available COM ports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на портите"));
        }
    }

    /**
     * Get available thermal printers
     */
    @GetMapping("/thermal-printers")
    public ResponseEntity<?> getAvailableThermalPrinters(@RequestHeader(value = "Admin-Code", required = false) String adminCode) {
        try {
            // Validate admin access
            if (adminCode == null || !printerService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неавторизиран пристап"));
            }

            List < String > printers = printerService.getAvailableThermalPrinters();
            return ResponseEntity.ok(Map.of("printers", printers));

        } catch (Exception e) {
            logger.error("Error getting available thermal printers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на принтерите"));
        }
    }

    /**
     * Initialize printers with current configuration
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializePrinters(@RequestHeader(value = "Admin-Code", required = false) String adminCode) {
        try {
            // Validate admin access
            if (adminCode == null || !printerService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неавторизиран пристап"));
            }

            printerService.initializePrinters();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Принтерите се иницијализирани успешно"
            ));

        } catch (Exception e) {
            logger.error("Error initializing printers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при иницијализација на принтерите"));
        }
    }

    /**
     * Get supported fiscal printer types
     */
    @GetMapping("/fiscal-types")
    public ResponseEntity<?> getSupportedFiscalPrinterTypes() {
        try {
            List < Map < String, String >> fiscalTypes = List.of(
                Map.of("value", "SYNERGY", "label", "Synergy PF-500/PF-550"),
                Map.of("value", "EXPERT_SX", "label", "David Expert SX"),
                Map.of("value", "FIDITEK", "label", "Fiditek Expert SX"),
                Map.of("value", "GENERIC", "label", "Generic Macedonian Fiscal Printer")
            );

            return ResponseEntity.ok(Map.of("fiscalTypes", fiscalTypes));

        } catch (Exception e) {
            logger.error("Error getting fiscal printer types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на типови принтери"));
        }
    }

    /**
     * Get supported thermal printer connection types
     */
    @GetMapping("/connection-types")
    public ResponseEntity<?> getSupportedConnectionTypes() {
        try {
            List < Map < String, String >> connectionTypes = List.of(
                Map.of("value", "USB", "label", "USB Connection"),
                Map.of("value", "NETWORK", "label", "Network/Ethernet Connection"),
                Map.of("value", "BLUETOOTH", "label", "Bluetooth Connection")
            );

            return ResponseEntity.ok(Map.of("connectionTypes", connectionTypes));

        } catch (Exception e) {
            logger.error("Error getting connection types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на типови врски"));
        }
    }
}