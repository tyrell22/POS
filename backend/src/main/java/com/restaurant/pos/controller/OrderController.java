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

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            List<Order> orders = orderService.getAllActiveOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error getting all orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на нарачките"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }

            Optional<Order> order = orderService.getOrderById(id);
            if (order.isPresent()) {
                return ResponseEntity.ok(order.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Нарачката не е пронајдена"));
            }
        } catch (Exception e) {
            logger.error("Error getting order by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на нарачката"));
        }
    }

    /**
     * ENHANCED: Get or create order for table with proper takeout support
     */
    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<?> getOrCreateOrderForTable(@PathVariable Integer tableNumber) {
        try {
            logger.info("Getting or creating order for table: {}", tableNumber);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }

            // ENHANCED: Validate table number with extended range
            if (tableNumber == null || tableNumber < 1) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }

            // Check if table number is too high
            if (tableNumber > 9999) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Бројот на маса не може да биде поголем од 9999"));
            }

            // Log table type for debugging
            boolean isTakeout = tableNumber >= 1000;
            String tableType = isTakeout ? "takeout" : "dine-in";
            logger.info("Processing {} order for table {}", tableType, tableNumber);

            Order order = orderService.getOrCreateOrderForTable(tableNumber);
            
            logger.info("Successfully {} order for table {} (ID: {}, Status: {})", 
                order.getCreatedAt().equals(order.getUpdatedAt()) ? "created" : "retrieved",
                tableNumber, order.getId(), order.getStatus());

            return ResponseEntity.ok(order);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid table number: {}", tableNumber, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Неважечки број на маса: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting/creating order for table: {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при креирање/вчитување на нарачката"));
        }
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<?> addItemToOrder(@PathVariable Long orderId, @Valid @RequestBody OrderItemRequest request) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Order order = orderService.addItemToOrder(orderId, request.getMenuItemId(), request.getQuantity(), request.getNotes());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            logger.error("Error adding item to order: {}", orderId, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error adding item to order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при додавање на производот"));
        }
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<?> removeItemFromOrder(@PathVariable Long orderId, @PathVariable Long itemId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Order order = orderService.removeItemFromOrder(orderId, itemId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            logger.error("Error removing item from order: {}", orderId, e);
            
            if (e.getMessage().contains("испратен")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("requiresAdmin", true, "error", e.getMessage()));
            }
            
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error removing item from order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при отстранување на производот"));
        }
    }

    @DeleteMapping("/{orderId}/items/{itemId}/admin")
    public ResponseEntity<?> removeItemFromOrderWithAdmin(@PathVariable Long orderId, @PathVariable Long itemId, @RequestBody Map<String, String> adminRequest) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            String adminCode = adminRequest.get("adminCode");
            if (adminCode == null || adminCode.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Админ кодот е задолжителен"));
            }
            
            if (!adminService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неточен админ код"));
            }
            
            Order order = orderService.removeItemFromOrderWithAdmin(orderId, itemId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            logger.error("Error removing item with admin: {}", orderId, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error removing item with admin: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при отстранување на производот"));
        }
    }

    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public ResponseEntity<?> updateItemQuantity(@PathVariable Long orderId, @PathVariable Long itemId, @RequestBody Map<String, Integer> quantityRequest) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Integer quantity = quantityRequest.get("quantity");
            if (quantity == null || quantity < 1) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Количината мора да биде најмалку 1"));
            }
            
            Order order = orderService.updateItemQuantity(orderId, itemId, quantity);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            logger.error("Error updating item quantity: {}", orderId, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating item quantity: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при ажурирање на количината"));
        }
    }

    @PostMapping("/{orderId}/send")
    public ResponseEntity<?> sendOrder(@PathVariable Long orderId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            orderService.sendOrder(orderId);
            return ResponseEntity.ok(Map.of("message", "Нарачката е испратена успешно"));
        } catch (RuntimeException e) {
            logger.error("Error sending order: {}", orderId, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error sending order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при испраќање на нарачката"));
        }
    }

    // Add this method to OrderController.java

/**
 * Move order from one table to another
 * Simple endpoint - no admin required
 */
@PostMapping("/{orderId}/move")
public ResponseEntity<?> moveOrderToTable(@PathVariable Long orderId, @RequestBody Map<String, Integer> moveRequest) {
    try {
        logger.info("Moving order {} to new table", orderId);
        
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }

        Integer newTableNumber = moveRequest.get("newTableNumber");
        if (newTableNumber == null || newTableNumber < 1 || newTableNumber > 9999) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Неважечки број на нова маса (1-9999)"));
        }

        Order movedOrder = orderService.moveOrderToTable(orderId, newTableNumber);
        
        logger.info("Successfully moved order {} to table {}", orderId, newTableNumber);
        
        return ResponseEntity.ok(Map.of(
            "message", "Нарачката е преместена успешно",
            "order", movedOrder,
            "oldTable", movedOrder.getTableNumber(), // This will be the new table now
            "newTable", newTableNumber
        ));
        
    } catch (RuntimeException e) {
        logger.error("Error moving order {}: {}", orderId, e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        logger.error("Unexpected error moving order {}", orderId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при преместување на нарачката"));
    }
}

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

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable String status) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Order.Status orderStatus = Order.Status.valueOf(status);
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Неважечки статус на нарачка"));
        } catch (Exception e) {
            logger.error("Error getting orders by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на нарачките"));
        }
    }

    /**
     * ENHANCED: Get debug info for troubleshooting takeout orders
     */
    @GetMapping("/debug/table/{tableNumber}")
    public ResponseEntity<?> getDebugInfoForTable(@PathVariable Integer tableNumber) {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("tableNumber", tableNumber);
            debugInfo.put("isTakeout", tableNumber >= 1000);
            debugInfo.put("isValidRange", tableNumber >= 1 && tableNumber <= 9999);
            debugInfo.put("databaseHealthy", databaseHealthService.isDatabaseHealthy());
            
            // Try to find existing order
            try {
                Optional<Order> existingOrder = orderService.findActiveOrderForTable(tableNumber);
                debugInfo.put("hasExistingOrder", existingOrder.isPresent());
                if (existingOrder.isPresent()) {
                    Order order = existingOrder.get();
                    debugInfo.put("existingOrderId", order.getId());
                    debugInfo.put("existingOrderStatus", order.getStatus());
                    debugInfo.put("existingOrderItems", order.getItems().size());
                }
            } catch (Exception e) {
                debugInfo.put("existingOrderError", e.getMessage());
            }
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Error getting debug info for table: {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при debug информации"));
        }
    }
}