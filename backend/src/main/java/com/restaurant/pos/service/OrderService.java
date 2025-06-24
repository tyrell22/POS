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
     * Get or create order for table - Returns the active order (OPEN or SENT)
     * Updated to support takeout orders (table numbers 1000+)
     */
    public Order getOrCreateOrderForTable(Integer tableNumber) {
        logger.info("Getting or creating order for table: {}", tableNumber);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            // Updated validation: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1) {
                throw new RuntimeException("Неважечки број на маса");
            }
            
            if (tableNumber > 1999) {
                throw new RuntimeException("Неважечки број на маса - премногу голем број");
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
                return sentOrder.get();
            }
            
            // No existing order, create new one
            logger.info("No existing order for table {}, creating new order", tableNumber);
            Order newOrder = new Order(tableNumber);
            Order savedOrder = orderRepository.save(newOrder);
            orderRepository.flush();
            logger.info("Created new order {} for table {}", savedOrder.getId(), tableNumber);
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error in getOrCreateOrderForTable for table {}: {}", tableNumber, e.getMessage(), e);
            throw new RuntimeException("Грешка при вчитување на нарачката: " + e.getMessage());
        }
    }

    /**
     * Create new order for table - Explicit method for creating new orders
     * Updated to support takeout orders
     */
    public Order createNewOrderForTable(Integer tableNumber) {
        logger.info("Creating new order for table: {}", tableNumber);
        
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            // Updated validation: 1-999 for dine-in, 1000-1999 for takeout
            if (tableNumber == null || tableNumber < 1) {
                throw new RuntimeException("Неважечки број на маса");
            }
            
            if (tableNumber > 1999) {
                throw new RuntimeException("Неважечки број на маса - премногу голем број");
            }
            
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
     * Add item to order - REWORKED to handle sent orders properly
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
            
            if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
                throw new RuntimeException("Не можете да додавате производи во затворена нарачка");
            }
            
            MenuItem menuItem = menuItemService.getItemById(request.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

            if (!menuItem.getAvailable()) {
                throw new RuntimeException("Производот не е достапен");
            }

            // Look for existing item with same menu item and notes
            Optional<OrderItem> existingItem = order.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(request.getMenuItemId())
                    && java.util.Objects.equals(item.getNotes(), request.getNotes()))
                .findFirst();

            if (existingItem.isPresent()) {
                // Update quantity for existing item
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

            // SMART STATUS: Always becomes OPEN when adding items
            order.setStatus(Order.Status.ОТВОРЕНА);

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Updated order {} total to {} with status {}", 
                savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getStatus());
            
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
     * Remove item from order - REWORKED to check sentQuantity
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

            // Check if item has sent quantity - if so, cannot remove without admin
            if (itemToRemove.hasSentQuantity()) {
                logger.warn("Cannot remove item {} with sent quantity {} from order {}", 
                    itemId, itemToRemove.getSentQuantity(), orderId);
                throw new RuntimeException("Не можете да отстраните производ кој е веќе испратен во кујна/бар. Потребен е админ код за оваа операција.");
            }

            order.getItems().remove(itemToRemove);
            orderItemRepository.delete(itemToRemove);
            logger.info("Removed item {} from order {}", itemId, orderId);

            // SMART STATUS MANAGEMENT: Update status based on remaining items
            updateOrderStatusBasedOnPendingItems(order);

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Updated order {} total after removal to {} with status {}", 
                savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getStatus());
            
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
     * Remove item from order with admin override
     */
    public Order removeItemFromOrderAdmin(Long orderId, Long itemId) {
        logger.info("Admin removing item {} from order {}", itemId, orderId);
        
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

            order.getItems().remove(itemToRemove);
            orderItemRepository.delete(itemToRemove);
            logger.info("Admin removed item {} from order {}", itemId, orderId);

            // SMART STATUS MANAGEMENT: Update status based on remaining items
            updateOrderStatusBasedOnPendingItems(order);

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error admin removing item {} from order {}: {}", itemId, orderId, e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при отстранување на производот: " + e.getMessage());
        }
    }

    /**
     * Update item quantity - FIXED to handle sentQuantity properly
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

            // Validate new quantity
            if (newQuantity <= 0) {
                return removeItemFromOrder(orderId, itemId);
            }

            // Check if trying to reduce below sent quantity
            if (item.hasSentQuantity() && newQuantity < item.getSentQuantity()) {
                logger.warn("Cannot reduce quantity below sent amount for item {} in order {}: sent={}, requested={}", 
                    itemId, orderId, item.getSentQuantity(), newQuantity);
                throw new RuntimeException("Не можете да ја намалите количината под веќе испратената количина (" + 
                    item.getSentQuantity() + " парчиња). Можете само да додадете повеќе.");
            }

            // Update quantity
            item.setQuantity(newQuantity);
            orderItemRepository.save(item);
            logger.info("Updated item {} quantity to {}", itemId, newQuantity);

            // SMART STATUS: Update based on pending items
            updateOrderStatusBasedOnPendingItems(order);

            order.calculateTotal();
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Updated order {} total after quantity change to {} with status {}", 
                savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getStatus());
            
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
     * Send order to kitchen/bar - REWORKED to mark sentQuantity
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

            // Mark all current pending quantities as sent
            for (OrderItem item : order.getItems()) {
                int pendingQty = item.getPendingQuantity();
                if (pendingQty > 0) {
                    item.markQuantityAsSent(pendingQty);
                    orderItemRepository.save(item);
                    logger.info("Marked {} units of item {} as sent (total sent now: {})", 
                        pendingQty, item.getId(), item.getSentQuantity());
                } else {
                    logger.info("Item {} has no pending quantity to send", item.getId());
                }
            }

            order.setStatus(Order.Status.ИСПРАТЕНА);
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            logger.info("Order {} status changed to SENT", savedOrder.getId());

            // Print tickets for pending items only
            try {
                printerService.printOrderTickets(savedOrder);
            } catch (Exception e) {
                logger.warn("Failed to print tickets for order {}: {}", savedOrder.getId(), e.getMessage());
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
     * Close order and print receipt
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
     * Updated to support takeout tables
     */
    public boolean tableHasActiveOrder(Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return false;
            }
            
            // Updated validation for takeout support
            if (tableNumber == null || tableNumber < 1 || tableNumber > 1999) {
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
     * Updated to support takeout tables
     */
    public String getTableStatus(Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return "UNKNOWN";
            }
            
            // Updated validation for takeout support
            if (tableNumber == null || tableNumber < 1 || tableNumber > 1999) {
                return "INVALID";
            }
            
            Optional<Order> openOrder = getCurrentActiveOrderForTable(tableNumber);
            if (openOrder.isPresent()) {
                return "OCCUPIED";
            }
            
            Optional<Order> sentOrder = getSentOrderForTable(tableNumber);
            if (sentOrder.isPresent()) {
                return "SENT";
            }
            
            return "AVAILABLE";
        } catch (Exception e) {
            logger.error("Error getting table status for table {}: {}", tableNumber, e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Helper method to intelligently update order status based on pending items
     */
    private void updateOrderStatusBasedOnPendingItems(Order order) {
        // Don't change status of closed orders
        if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
            return;
        }

        // Check if there are any pending (unsent) items
        boolean hasPendingItems = order.getItems().stream()
            .anyMatch(item -> {
                Integer sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                Integer currentQty = item.getQuantity() != null ? item.getQuantity() : 0;
                boolean hasPending = currentQty > sentQty;
                logger.debug("Item {}: qty={}, sent={}, pending={}", item.getId(), currentQty, sentQty, hasPending);
                return hasPending;
            });

        if (hasPendingItems) {
            // There are pending items - order should be OPEN
            if (order.getStatus() != Order.Status.ОТВОРЕНА) {
                logger.info("Order {} has pending items, changing status from {} to OPEN", order.getId(), order.getStatus());
                order.setStatus(Order.Status.ОТВОРЕНА);
            }
        } else {
            // No pending items - if order was previously sent, keep it as SENT
            boolean hasAnySentItems = order.getItems().stream()
                .anyMatch(item -> {
                    Integer sentQty = item.getSentQuantity() != null ? item.getSentQuantity() : 0;
                    return sentQty > 0;
                });
            
            if (hasAnySentItems && order.getStatus() != Order.Status.ИСПРАТЕНА) {
                logger.info("Order {} has no pending items but has sent items, changing status from {} to SENT", order.getId(), order.getStatus());
                order.setStatus(Order.Status.ИСПРАТЕНА);
            }
        }
    }
}