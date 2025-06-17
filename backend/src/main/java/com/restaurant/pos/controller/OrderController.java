package com.restaurant.pos.controller;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.dto.OrderItemRequest;
import com.restaurant.pos.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping
    public ResponseEntity<List<Order>> getAllActiveOrders() {
        List<Order> orders = orderService.getAllActiveOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        try {
            Order.Status orderStatus = Order.Status.valueOf(status);
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
            .map(order -> ResponseEntity.ok(order))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<Order> getOrCreateOrderForTable(@PathVariable Integer tableNumber) {
        if (tableNumber < 1 || tableNumber > 12) {
            return ResponseEntity.badRequest().build();
        }
        
        Order order = orderService.getOrCreateOrderForTable(tableNumber);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{orderId}/items")
    public ResponseEntity<?> addItemToOrder(@PathVariable Long orderId, 
                                           @Valid @RequestBody OrderItemRequest request) {
        try {
            Order updatedOrder = orderService.addItemToOrder(orderId, request);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<?> removeItemFromOrder(@PathVariable Long orderId, @PathVariable Long itemId) {
        try {
            Order updatedOrder = orderService.removeItemFromOrder(orderId, itemId);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public ResponseEntity<?> updateItemQuantity(@PathVariable Long orderId, 
                                              @PathVariable Long itemId,
                                              @RequestBody Map<String, Integer> request) {
        try {
            Integer newQuantity = request.get("quantity");
            if (newQuantity == null || newQuantity < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Неважечка количина"));
            }
            
            Order updatedOrder = orderService.updateItemQuantity(orderId, itemId, newQuantity);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{orderId}/send")
    public ResponseEntity<?> sendOrderToKitchen(@PathVariable Long orderId) {
        try {
            Order sentOrder = orderService.sendOrderToKitchen(orderId);
            return ResponseEntity.ok(Map.of(
                "message", "Нарачката е испратена успешно",
                "order", sentOrder
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{orderId}/close")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId) {
        try {
            orderService.closeOrder(orderId);
            return ResponseEntity.ok(Map.of("message", "Нарачката е затворена успешно"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}