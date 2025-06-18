package com.restaurant.pos.controller;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.dto.OrderItemRequest;
import com.restaurant.pos.service.OrderService;
import com.restaurant.pos.service.DatabaseHealthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            
            // Validate table number
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
                logger.warn("Invalid table number: {}", tableNumber);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            Order order = orderService.getOrCreateOrderForTable(tableNumber);
            logger.info("Successfully got/created order {} for table {}", order.getId(), tableNumber);
            
            // Enhanced response with status information
            Map<String, Object> response = Map.of(
                "id", order.getId(),
                "tableNumber", order.getTableNumber(),
                "status", order.getStatus().toString(),
                "totalAmount", order.getTotalAmount(),
                "items", order.getItems(),
                "createdAt", order.getCreatedAt(),
                "updatedAt", order.getUpdatedAt(),
                // Additional metadata for frontend handling
                "canModify", order.getStatus() == Order.Status.ОТВОРЕНА,
                "isSent", order.getStatus() == Order.Status.ИСПРАТЕНА,
                "isClosed", order.getStatus() == Order.Status.ЗАТВОРЕНА
            );
            
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
    
    // NEW ENDPOINT: Create new order for table (explicit)
    @PostMapping("/table/{tableNumber}/new")
    public ResponseEntity<?> createNewOrderForTable(@PathVariable Integer tableNumber) {
        try {
            logger.info("Creating new order for table: {}", tableNumber);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
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
            
            // Validate request
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
            
            // Enhanced error handling with specific response codes
            if (e.getMessage().contains("испратена нарачка")) {
                return ResponseEntity.status(HttpStatus.CONFLICT) // 409 Conflict
                    .body(Map.of(
                        "error", e.getMessage(),
                        "code", "ORDER_ALREADY_SENT",
                        "suggestion", "Започнете нова нарачка за оваа маса"
                    ));
            } else if (e.getMessage().contains("затворена нарачка")) {
                return ResponseEntity.status(HttpStatus.GONE) // 410 Gone
                    .body(Map.of(
                        "error", e.getMessage(),
                        "code", "ORDER_CLOSED"
                    ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
            }
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
            
            if (e.getMessage().contains("испратена нарачка") || e.getMessage().contains("затворена нарачка")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
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
            
            if (e.getMessage().contains("испратена нарачка") || e.getMessage().contains("затворена нарачка")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
            }
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
    
    // NEW ENDPOINTS: Enhanced table order management
    
    @GetMapping("/table/{tableNumber}/status")
    public ResponseEntity<?> getTableOrderStatus(@PathVariable Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки број на маса"));
            }
            
            String status = orderService.getTableStatus(tableNumber);
            Optional<Order> openOrder = orderService.getCurrentActiveOrderForTable(tableNumber);
            Optional<Order> sentOrder = orderService.getSentOrderForTable(tableNumber);
            
            Map<String, Object> response = Map.of(
                "tableNumber", tableNumber,
                "status", status,
                "hasActiveOrder", orderService.tableHasActiveOrder(tableNumber),
                "openOrder", openOrder.orElse(null),
                "sentOrder", sentOrder.orElse(null)
            );
            
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
            
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
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
            
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
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