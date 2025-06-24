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
    public ResponseEntity<?> getAllActiveOrders() {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            List<Order> orders = orderService.getAllActiveOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error getting all active orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на нарачките"));
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
            logger.warn("Invalid order status: {}", status);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Неважечки статус на нарачка"));
        } catch (Exception e) {
            logger.error("Error getting orders by status: {}", status, e);
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
            
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isPresent()) {
                return ResponseEntity.ok(orderOpt.get());
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
    
    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<?> getOrCreateOrderForTable(@PathVariable Integer tableNumber) {
        try {
            logger.info("Getting or creating order for table: {}", tableNumber);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                logger.error("Database is not healthy for table: {}", tableNumber);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate table numbers: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1) {
                logger.warn("Invalid table number: {}", tableNumber);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            // Allow regular tables (1-999) and takeout tables (1000-1999)
            if (tableNumber > 1999) {
                logger.warn("Table number too high: {}", tableNumber);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            Order order = orderService.getOrCreateOrderForTable(tableNumber);
            logger.info("Successfully got/created order {} for table {}", order.getId(), tableNumber);
            
            // Enhanced response with status information using HashMap for more than 10 entries
            Map<String, Object> response = new HashMap<>();
            response.put("id", order.getId());
            response.put("tableNumber", order.getTableNumber());
            response.put("status", order.getStatus().toString());
            response.put("totalAmount", order.getTotalAmount());
            response.put("items", order.getItems());
            response.put("createdAt", order.getCreatedAt());
            response.put("updatedAt", order.getUpdatedAt());
            response.put("canModify", order.getStatus() == Order.Status.ОТВОРЕНА);
            response.put("isSent", order.getStatus() == Order.Status.ИСПРАТЕНА);
            response.put("isClosed", order.getStatus() == Order.Status.ЗАТВОРЕНА);
            response.put("isTakeout", tableNumber >= 1000); // Flag for takeout orders
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error getting/creating order for table {}: {}", tableNumber, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting/creating order for table {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на нарачката"));
        }
    }
    
    @PostMapping("/table/{tableNumber}/new")
    public ResponseEntity<?> createNewOrderForTable(@PathVariable Integer tableNumber) {
        try {
            logger.info("Creating new order for table: {}", tableNumber);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate table numbers: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1 || tableNumber > 1999) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            Order newOrder = orderService.createNewOrderForTable(tableNumber);
            logger.info("Successfully created new order {} for table {}", newOrder.getId(), tableNumber);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error creating new order for table {}: {}", tableNumber, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating new order for table {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при креирање на нова нарачка"));
        }
    }
    
    @PostMapping("/{orderId}/items")
    public ResponseEntity<?> addItemToOrder(@PathVariable Long orderId, 
                                           @Valid @RequestBody OrderItemRequest request) {
        try {
            logger.info("Adding item to order {}: menuItemId={}, quantity={}", 
                orderId, request.getMenuItemId(), request.getQuantity());
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            if (orderId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID на нарачка е задолжително"));
            }
            
            if (request.getMenuItemId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID на производ е задолжително"));
            }
            
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Количината мора да биде позитивна"));
            }
            
            Order updatedOrder = orderService.addItemToOrder(orderId, request);
            logger.info("Successfully added item to order {}", orderId);
            
            return ResponseEntity.ok(updatedOrder);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error adding item to order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error adding item to order {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при додавање на производот"));
        }
    }
    
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<?> removeItemFromOrder(@PathVariable Long orderId, @PathVariable Long itemId) {
        try {
            logger.info("Removing item {} from order {}", itemId, orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Order updatedOrder = orderService.removeItemFromOrder(orderId, itemId);
            logger.info("Successfully removed item {} from order {}", itemId, orderId);
            
            return ResponseEntity.ok(updatedOrder);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error removing item {} from order {}: {}", itemId, orderId, e.getMessage(), e);
            
            if (e.getMessage().contains("админ код")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error", e.getMessage(),
                        "requiresAdmin", true,
                        "code", "ADMIN_REQUIRED_FOR_SENT_ITEM"
                    ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Unexpected error removing item {} from order {}", itemId, orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при отстранување на производот"));
        }
    }
    
    // NEW: Admin remove item endpoint
    @DeleteMapping("/{orderId}/items/{itemId}/admin")
    public ResponseEntity<?> removeItemFromOrderAdmin(@PathVariable Long orderId, 
                                                     @PathVariable Long itemId,
                                                     @RequestBody Map<String, String> adminRequest) {
        try {
            logger.info("Admin removing item {} from order {}", itemId, orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            String adminCode = adminRequest.get("adminCode");
            if (adminCode == null || adminCode.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Админ кодот е задолжителен"));
            }
            
            // Validate admin code
            if (!adminService.validateAdminCode(adminCode)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неточен админ код"));
            }
            
            Order updatedOrder = orderService.removeItemFromOrderAdmin(orderId, itemId);
            logger.info("Successfully admin removed item {} from order {}", itemId, orderId);
            
            return ResponseEntity.ok(updatedOrder);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error admin removing item {} from order {}: {}", itemId, orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error admin removing item {} from order {}", itemId, orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при отстранување на производот"));
        }
    }
    
    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public ResponseEntity<?> updateItemQuantity(@PathVariable Long orderId, 
                                              @PathVariable Long itemId,
                                              @RequestBody Map<String, Integer> request) {
        try {
            logger.info("Updating item {} quantity in order {}", itemId, orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Integer newQuantity = request.get("quantity");
            if (newQuantity == null || newQuantity < 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечка количина"));
            }
            
            Order updatedOrder = orderService.updateItemQuantity(orderId, itemId, newQuantity);
            logger.info("Successfully updated item {} quantity in order {}", itemId, orderId);
            
            return ResponseEntity.ok(updatedOrder);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error updating item {} quantity in order {}: {}", itemId, orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating item {} quantity in order {}", itemId, orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при ажурирање на количината"));
        }
    }
    
    @PostMapping("/{orderId}/send")
    public ResponseEntity<?> sendOrderToKitchen(@PathVariable Long orderId) {
        try {
            logger.info("Sending order {} to kitchen", orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Order sentOrder = orderService.sendOrderToKitchen(orderId);
            logger.info("Successfully sent order {} to kitchen", orderId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Нарачката е испратена успешно",
                "order", sentOrder
            ));
            
        } catch (RuntimeException e) {
            logger.error("Runtime error sending order {} to kitchen: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error sending order {} to kitchen", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при испраќање на нарачката"));
        }
    }
    
    @PostMapping("/{orderId}/close")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId) {
        try {
            logger.info("Closing order {}", orderId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            orderService.closeOrder(orderId);
            logger.info("Successfully closed order {}", orderId);
            
            return ResponseEntity.ok(Map.of("message", "Нарачката е затворена успешно"));
            
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
    
    @GetMapping("/table/{tableNumber}/status")
    public ResponseEntity<?> getTableOrderStatus(@PathVariable Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate table numbers: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1 || tableNumber > 1999) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            String status = orderService.getTableStatus(tableNumber);
            Optional<Order> openOrder = orderService.getCurrentActiveOrderForTable(tableNumber);
            Optional<Order> sentOrder = orderService.getSentOrderForTable(tableNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tableNumber", tableNumber);
            response.put("status", status);
            response.put("hasActiveOrder", orderService.tableHasActiveOrder(tableNumber));
            response.put("openOrder", openOrder.orElse(null));
            response.put("sentOrder", sentOrder.orElse(null));
            response.put("isTakeout", tableNumber >= 1000);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting table order status for table {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при проверка на статусот"));
        }
    }
    
    @GetMapping("/table/{tableNumber}/active")
    public ResponseEntity<?> getActiveOrderForTable(@PathVariable Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate table numbers: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1 || tableNumber > 1999) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            Optional<Order> activeOrder = orderService.getCurrentActiveOrderForTable(tableNumber);
            
            if (activeOrder.isPresent()) {
                return ResponseEntity.ok(activeOrder.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Нема активна нарачка за оваа маса"));
            }
            
        } catch (Exception e) {
            logger.error("Error getting active order for table {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на активната нарачка"));
        }
    }
    
    @GetMapping("/table/{tableNumber}/sent")
    public ResponseEntity<?> getSentOrderForTable(@PathVariable Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate table numbers: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1 || tableNumber > 1999) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            Optional<Order> sentOrder = orderService.getSentOrderForTable(tableNumber);
            
            if (sentOrder.isPresent()) {
                return ResponseEntity.ok(sentOrder.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Нема испратена нарачка за оваа маса"));
            }
            
        } catch (Exception e) {
            logger.error("Error getting sent order for table {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на испратената нарачка"));
        }
    }
}