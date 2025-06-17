package com.restaurant.pos.service;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.entity.OrderItem;
import com.restaurant.pos.entity.MenuItem;
import com.restaurant.pos.dto.OrderItemRequest;
import com.restaurant.pos.repository.OrderRepository;
import com.restaurant.pos.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private PrinterService printerService;

    public List<Order> getAllActiveOrders() {
        return orderRepository.findAllActiveOrders();
    }

    public List<Order> getOrdersByStatus(Order.Status status) {
        return orderRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Order getOrCreateOrderForTable(Integer tableNumber) {
    // Look for both OPEN and SENT orders
    Optional<Order> existingOrder = orderRepository.findByTableNumberAndStatus(
        tableNumber, Order.Status.ОТВОРЕНА);
    
    if (!existingOrder.isPresent()) {
        existingOrder = orderRepository.findByTableNumberAndStatus(
            tableNumber, Order.Status.ИСПРАТЕНА);
    }

    if (existingOrder.isPresent()) {
        return existingOrder.get();
    }
    
    Order newOrder = new Order(tableNumber);
    return orderRepository.save(newOrder);
}

    public Order addItemToOrder(Long orderId, OrderItemRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));
        
        MenuItem menuItem = menuItemService.getItemById(request.getMenuItemId())
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

        if (!menuItem.getAvailable()) {
            throw new RuntimeException("Производот не е достапен");
        }

        // Check if item with same notes already exists
        Optional < OrderItem > existingItem = order.getItems().stream()
            .filter(item -> item.getMenuItem().getId().equals(request.getMenuItemId())
                && java.util.Objects.equals(item.getNotes(), request.getNotes()))
            .findFirst();

        if (existingItem.isPresent()) {
            // Update existing item quantity
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            orderItemRepository.save(item);
        } else {
            // Create new item
            OrderItem newItem = new OrderItem(order, menuItem, request.getQuantity(), request.getNotes());
            order.getItems().add(newItem);
            orderItemRepository.save(newItem);
        }

        order.calculateTotal();
        return orderRepository.save(order);
    }

    public Order removeItemFromOrder(Long orderId, Long itemId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));
        
        OrderItem itemToRemove = order.getItems().stream()
            .filter(item -> item.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден во нарачката"));

        order.getItems().remove(itemToRemove);
        orderItemRepository.delete(itemToRemove);

        order.calculateTotal();
        return orderRepository.save(order);
    }

    public Order updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        if (newQuantity <= 0) {
            return removeItemFromOrder(orderId, itemId);
        }
        
        OrderItem item = order.getItems().stream()
            .filter(oi -> oi.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

        item.setQuantity(newQuantity);
        orderItemRepository.save(item);

        order.calculateTotal();
        return orderRepository.save(order);
    }

    public Order sendOrderToKitchen(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        if (order.getItems().isEmpty()) {
            throw new RuntimeException("Не можете да испратите празна нарачка");
        }

        order.setStatus(Order.Status.ИСПРАТЕНА);
        Order savedOrder = orderRepository.save(order);

        // Print tickets
        printerService.printOrderTickets(savedOrder);

        return savedOrder;
    }

    public void closeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        order.setStatus(Order.Status.ЗАТВОРЕНА);
        orderRepository.save(order);

        // Print receipt
        printerService.printReceipt(order);
    }
}