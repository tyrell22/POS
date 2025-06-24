package com.restaurant.pos.controller;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.dto.OrderItemRequest;
import com.restaurant.pos.service.OrderService;
import com.restaurant.pos.service.DatabaseHealthService;
import com.restaurant.pos.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private DatabaseHealthService databaseHealthService;
    
    @Autowired
    private AdminService adminService;
    
    // ... (keep all existing methods unchanged until close order endpoints)
    
    /**
     * Close order with thermal printer (default)
     */
    @PostMapping("/{orderId}/close")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId) {
        try {
            logger.info("Closing order {} with thermal printer", orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            orderService.closeOrder(orderId, false); // Use thermal printer
            logger.info("Successfully closed order {} with thermal printer", orderId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Нарачката е затворена успешно (термален принтер)",
                "receiptType", "thermal"
            ));
            
        } catch (RuntimeException e) {
            logger.error("Runtime error closing order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error closing order {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при затворање на нарачката"));
        }
    }
    
    /**
     * Close order with fiscal printer (when F key is held)
     */
    @PostMapping("/{orderId}/close-fiscal")
    public ResponseEntity<?> closeOrderWithFiscalPrinter(@PathVariable Long orderId) {
        try {
            logger.info("Closing order {} with fiscal printer", orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            orderService.closeOrder(orderId, true); // Use fiscal printer
            logger.info("Successfully closed order {} with fiscal printer", orderId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Нарачката е затворена успешно (фискален принтер)",
                "receiptType", "fiscal"
            ));
            
        } catch (RuntimeException e) {
            logger.error("Runtime error closing order {} with fiscal printer: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error closing order {} with fiscal printer", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при затворање на нарачката со фискален принтер"));
        }
    }
    
    // ... (keep all other existing methods unchanged)
}