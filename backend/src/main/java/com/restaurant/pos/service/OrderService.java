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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private PrinterService printerService;

    @Autowired
    private DatabaseHealthService databaseHealthService;

    public List<Order> getAllActiveOrders() {
        return orderRepository.findAllActiveOrders();
    }

    public List<Order> getOrdersByStatus(Order.Status status) {
        return orderRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * Get or create order for table - ENHANCED to handle multiple orders
     */
    public Order getOrCreateOrderForTable(Integer tableNumber) {
        logger.info("Getting or creating order for table: {}", tableNumber);
        
        try {
            // Check database health first
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            // Validate table number
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
                throw new RuntimeException("Неважечки број на маса");
            }
            
            // Look for an OPEN order first
            Optional<Order> openOrder = orderRepository.findByTableNumberAndStatus(
                tableNumber, Order.Status.ОТВОРЕНА);
            
            if (openOrder.isPresent()) {
                logger.info("Found existing open order for table {}: {}", tableNumber, openOrder.get().getId());
                return openOrder.get();
            }
            
            // Look for a SENT order
            Optional<Order> sentOrder = orderRepository.findByTableNumberAndStatus(
                tableNumber, Order.Status.ИСПРАТЕНА);
            
            if (sentOrder.isPresent()) {
                logger.info("Found sent order for table {}: {}", tableNumber, sentOrder.get().getId());
                // Return the SENT order to allow modifications
                return sentOrder.get();
            }
            
            // Check for multiple active orders to prevent duplicates
            List<Order> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(Order.Status.ОТВОРЕНА, Order.Status.ИСПРАТЕНА));
            activeOrders = activeOrders.stream()
                .filter(o -> o.getTableNumber().equals(tableNumber))
                .toList();
            
            if (activeOrders.size() > 1) {
                logger.warn("Multiple active orders found for table {}: {}", tableNumber, activeOrders);
                // Select the most recent order
                Order mostRecent = activeOrders.stream()
                    .max(Comparator.comparing(Order::getCreatedAt))
                    .orElseThrow(() -> new RuntimeException("No valid order found"));
                logger.info("Selecting most recent order {} for table {}", mostRecent.getId(), tableNumber);
                return mostRecent;
            }
            
            // No existing order, create new one
            logger.info("No existing order for table {}, creating new order", tableNumber);
            Order newOrder = new Order(tableNumber);
            Order savedOrder = orderRepository.save(newOrder);
            orderRepository.flush(); // Ensure immediate persistence
            logger.info("Created new order {} for table {}", savedOrder.getId(), tableNumber);
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error in getOrCreateOrderForTable for table {}: {}", tableNumber, e.getMessage(), e);
            throw new RuntimeException("Грешка при вчитување на нарачката: " + e.getMessage());
        }
    }

    /**
     * Create new order for table - Explicit method for creating new orders
     */
    public Order createNewOrderForTable(Integer tableNumber) {
        logger.info("Creating new order for table: {}", tableNumber);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            if (tableNumber == null || tableNumber < 1 || tableNumber > 100) {
                throw new RuntimeException("Неважечки број на маса");
            }
            
            // Create new order regardless of existing orders
            Order newOrder = new Order(tableNumber);
            Order savedOrder = orderRepository.save(newOrder);
            orderRepository.flush();
            logger.info("Created new order {} for table {}", savedOrder.getId(), tableNumber);
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error creating new order for table {}: {}", tableNumber, e.getMessage(), e);
            throw new RuntimeException("Грешка при креирање на нова нарачка: " + e.getMessage());
        }
    }

    /**
     * Add item to order - FIXED to always create new items for sent orders
     */
    public Order addItemToOrder(Long orderId, OrderItemRequest request) {
        logger.info("Adding item to order {}: menuItem={}, quantity={}", 
            orderId, request.getMenuItemId(), request.getQuantity());
            
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));
            
            boolean wasOrderSent = order.getStatus() == Order.Status.ИСПРАТЕНА;
            
            // Handle SENT orders: allow adding new items, preserve existing items
            if (order.getStatus() == Order.Status.ИСПРАТЕНА) {
                logger.info("Changing SENT order {} to OPEN for adding new item", orderId);
                // Mark all existing items as sent before changing status
                markAllItemsAsSent(order);
                order.setStatus(Order.Status.ОТВОРЕНА);
                orderRepository.save(order);
                orderRepository.flush();
            }
            
            if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
                throw new RuntimeException("Не можете да додавате производи во затворена нарачка");
            }
            
            MenuItem menuItem = menuItemService.getItemById(request.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

            if (!menuItem.getAvailable()) {
                throw new RuntimeException("Производот не е достапен");
            }

            // FIXED: For orders that were SENT, ALWAYS create new items
            // For OPEN orders, check if we can merge with existing non-sent items
            if (wasOrderSent) {
                // Always create new item for previously sent orders
                logger.info("Creating new item for previously sent order {}", orderId);
                OrderItem newItem = new OrderItem(order, menuItem, request.getQuantity(), request.getNotes());
                order.getItems().add(newItem);
                orderItemRepository.save(newItem);
                logger.info("Added new item {} to order {}", newItem.getId(), orderId);
            } else {
                // For OPEN orders, try to merge with existing non-sent items
                Optional<OrderItem> existingItem = order.getItems().stream()
                    .filter(item -> item.getMenuItem().getId().equals(request.getMenuItemId())
                        && java.util.Objects.equals(item.getNotes(), request.getNotes())
                        && !isItemSent(item))  // Only merge with non-sent items
                    .findFirst();

                if (existingItem.isPresent()) {
                    // Update quantity for existing non-sent item
                    OrderItem item = existingItem.get();
                    item.setQuantity(item.getQuantity() + request.getQuantity());
                    orderItemRepository.save(item);
                    logger.info("Updated existing item {} quantity to {}", item.getId(), item.getQuantity());
                } else {
                    // Create new OrderItem
                    OrderItem newItem = new OrderItem(order, menuItem, request.getQuantity(), request.getNotes());
                    order.getItems().add(newItem);
                    orderItemRepository.save(newItem);
                    logger.info("Added new item {} to order {}", newItem.getId(), orderId);
                }
            }

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Updated order {} total to {}", savedOrder.getId(), savedOrder.getTotalAmount());
            
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error adding item to order {}: {}", orderId, e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при додавање на производот: " + e.getMessage());
        }
    }

    /**
     * Remove item from order - ENHANCED to prevent removing sent items
     */
    public Order removeItemFromOrder(Long orderId, Long itemId) {
        logger.info("Removing item {} from order {}", itemId, orderId);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));
            
            if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
                throw new RuntimeException("Не можете да менувате затворена нарачка");
            }
            
            OrderItem itemToRemove = order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден во нарачката"));

            // FIXED: Check if this item is sent
            if (isItemSent(itemToRemove)) {
                logger.warn("Cannot remove sent item {} from order {}", itemId, orderId);
                throw new RuntimeException("Не можете да отстраните веќе испратен производ од нарачката");
            }

            order.getItems().remove(itemToRemove);
            orderItemRepository.delete(itemToRemove);
            logger.info("Removed item {} from order {}", itemId, orderId);

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Updated order {} total after removal to {}", savedOrder.getId(), savedOrder.getTotalAmount());
            
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error removing item {} from order {}: {}", itemId, orderId, e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при отстранување на производот: " + e.getMessage());
        }
    }

    /**
     * Update item quantity - FIXED to prevent modifying sent items
     */
    public Order updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        logger.info("Updating item {} quantity to {} in order {}", itemId, newQuantity, orderId);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
                throw new RuntimeException("Не можете да менувате затворена нарачка");
            }

            OrderItem item = order.getItems().stream()
                .filter(oi -> oi.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

            // FIXED: Check if this item is sent
            if (isItemSent(item)) {
                if (newQuantity < item.getQuantity()) {
                    logger.warn("Cannot reduce quantity for sent item {} in order {} from {} to {}", 
                        itemId, orderId, item.getQuantity(), newQuantity);
                    throw new RuntimeException("Не можете да ја намалите количината на веќе испратен производ");
                }
                if (newQuantity == item.getQuantity()) {
                    logger.warn("No change in quantity for sent item {} in order {}", itemId, orderId);
                    return order; // No change needed
                }
                // For sent items, create new item for additional quantity
                logger.info("Creating new item for additional quantity {} for sent item {}", 
                    newQuantity - item.getQuantity(), itemId);
                OrderItem newItem = new OrderItem(
                    order, 
                    item.getMenuItem(), 
                    newQuantity - item.getQuantity(), 
                    item.getNotes()
                );
                order.getItems().add(newItem);
                orderItemRepository.save(newItem);
                logger.info("Added new item {} for additional quantity {} in order {}", 
                    newItem.getId(), newQuantity - item.getQuantity(), orderId);
            } else {
                // For non-sent items, update quantity directly
                if (newQuantity <= 0) {
                    return removeItemFromOrder(orderId, itemId);
                }
                item.setQuantity(newQuantity);
                orderItemRepository.save(item);
                logger.info("Updated item {} quantity to {}", itemId, newQuantity);
            }

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Updated order {} total after quantity change to {}", savedOrder.getId(), savedOrder.getTotalAmount());
            
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error updating item {} quantity in order {}: {}", itemId, orderId, e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при ажурирање на количината: " + e.getMessage());
        }
    }

    /**
     * Send order to kitchen/bar - ENHANCED to mark items as sent
     */
    public Order sendOrderToKitchen(Long orderId) {
        logger.info("Sending order {} to kitchen", orderId);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            if (order.getStatus() != Order.Status.ОТВОРЕНА) {
                throw new RuntimeException("Можете да испраќате само отворени нарачки");
            }

            if (order.getItems().isEmpty()) {
                throw new RuntimeException("Не можете да испратите празна нарачка");
            }

            // Mark all current items as sent
            markAllItemsAsSent(order);

            order.setStatus(Order.Status.ИСПРАТЕНА);
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Order {} status changed to SENT", savedOrder.getId());

            // Print tickets
            try {
                printerService.printOrderTickets(savedOrder);
            } catch (Exception e) {
                logger.warn("Failed to print tickets for order {}: {}", savedOrder.getId(), e.getMessage());
                // Don't fail the entire operation if printing fails
            }

            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error sending order {} to kitchen: {}", orderId, e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при испраќање на нарачката: " + e.getMessage());
        }
    }

    /**
     * Close order and print receipt - Only allow closing SENT orders
     */
    public void closeOrder(Long orderId) {
        logger.info("Closing order {}", orderId);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            if (order.getStatus() != Order.Status.ИСПРАТЕНА) {
                throw new RuntimeException("Можете да затворате само испратени нарачки");
            }

            order.setStatus(Order.Status.ЗАТВОРЕНА);
            orderRepository.save(order);
            orderRepository.flush();
            logger.info("Order {} status changed to CLOSED", orderId);

            // Print receipt
            try {
                printerService.printReceipt(order);
            } catch (Exception e) {
                logger.warn("Failed to print receipt for order {}: {}", orderId, e.getMessage());
                // Don't fail the entire operation if printing fails
            }
            
        } catch (Exception e) {
            logger.error("Error closing order {}: {}", orderId, e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при затворање на нарачката: " + e.getMessage());
        }
    }

    /**
     * Get current active order for table (only OPEN orders)
     */
    public Optional<Order> getCurrentActiveOrderForTable(Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return Optional.empty();
            }
            return orderRepository.findByTableNumberAndStatus(tableNumber, Order.Status.ОТВОРЕНА);
        } catch (Exception e) {
            logger.error("Error getting current active order for table {}: {}", tableNumber, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get sent order for table (for closing)
     */
    public Optional<Order> getSentOrderForTable(Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return Optional.empty();
            }
            return orderRepository.findByTableNumberAndStatus(tableNumber, Order.Status.ИСПРАТЕНА);
        } catch (Exception e) {
            logger.error("Error getting sent order for table {}: {}", tableNumber, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if table has any active orders (OPEN or SENT)
     */
    public boolean tableHasActiveOrder(Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return false;
            }
            
            Optional<Order> openOrder = orderRepository.findByTableNumberAndStatus(
                tableNumber, Order.Status.ОТВОРЕНА);
            Optional<Order> sentOrder = orderRepository.findByTableNumberAndStatus(
                tableNumber, Order.Status.ИСПРАТЕНА);
            
            return openOrder.isPresent() || sentOrder.isPresent();
        } catch (Exception e) {
            logger.error("Error checking if table {} has active order: {}", tableNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Get table status based on orders
     */
    public String getTableStatus(Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return "UNKNOWN";
            }
            
            Optional<Order> openOrder = getCurrentActiveOrderForTable(tableNumber);
            if (openOrder.isPresent()) {
                return "OCCUPIED"; // Table has open order
            }
            
            Optional<Order> sentOrder = getSentOrderForTable(tableNumber);
            if (sentOrder.isPresent()) {
                return "SENT"; // Table has sent order (ready for payment)
            }
            
            return "AVAILABLE"; // No active orders
        } catch (Exception e) {
            logger.error("Error getting table status for table {}: {}", tableNumber, e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * FIXED: Helper method to check if an item is sent
     * This uses a simple approach - you could enhance with actual sent tracking
     */
    private boolean isItemSent(OrderItem item) {
        // Check if item has a sent timestamp/flag
        // For now, we'll use a simple approach: if the item was created before the order was last sent
        
        // If you add sentAt field to OrderItem:
        // return item.getSentAt() != null;
        
        // If you add sentQuantity field to OrderItem:
        // return item.getSentQuantity() != null && item.getSentQuantity() > 0;
        
        // Simple approach: check if item has a sent marker
        // You could store this in notes or add a separate field
        return item.getNotes() != null && item.getNotes().contains("__SENT__");
    }

    /**
     * Mark all items in order as sent
     */
    private void markAllItemsAsSent(Order order) {
        logger.info("Marking all items as sent for order {}", order.getId());
        
        order.getItems().forEach(item -> {
            logger.info("Item {} with quantity {} is being sent", item.getId(), item.getQuantity());
            
            // Simple approach: mark in notes
            String currentNotes = item.getNotes() != null ? item.getNotes() : "";
            if (!currentNotes.contains("__SENT__")) {
                item.setNotes(currentNotes + "__SENT__");
                orderItemRepository.save(item);
            }
            
            // If you add sentAt field: item.setSentAt(LocalDateTime.now());
            // If you add sentQuantity field: item.setSentQuantity(item.getQuantity());
        });
    }
}