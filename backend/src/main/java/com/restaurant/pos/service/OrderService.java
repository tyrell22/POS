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

    @Transactional(readOnly = true)
    public List<Order> getAllActiveOrders() {
        try {
            return orderRepository.findAllActiveOrders();
        } catch (Exception e) {
            logger.error("Error getting all active orders", e);
            throw new RuntimeException("Грешка при вчитување на активните нарачки");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        try {
            return orderRepository.findById(id);
        } catch (Exception e) {
            logger.error("Error getting order by ID: {}", id, e);
            throw new RuntimeException("Грешка при вчитување на нарачката");
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.Status status) {
        try {
            return orderRepository.findByStatusOrderByCreatedAtAsc(status);
        } catch (Exception e) {
            logger.error("Error getting orders by status: {}", status, e);
            throw new RuntimeException("Грешка при вчитување на нарачките по статус");
        }
    }

    /**
     * ENHANCED: Find active order for table (both dine-in and takeout)
     */
    @Transactional(readOnly = true)
    public Optional<Order> findActiveOrderForTable(Integer tableNumber) {
        try {
            // Look for orders with status ОТВОРЕНА or ИСПРАТЕНА
            List<Order.Status> activeStatuses = List.of(Order.Status.ОТВОРЕНА, Order.Status.ИСПРАТЕНА);
            List<Order> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(activeStatuses);
            
            return activeOrders.stream()
                .filter(order -> order.getTableNumber().equals(tableNumber))
                .findFirst();
        } catch (Exception e) {
            logger.error("Error finding active order for table: {}", tableNumber, e);
            throw new RuntimeException("Грешка при барање на активна нарачка");
        }
    }

    /**
     * ENHANCED: Get or create order for table with better takeout support
     */
    public Order getOrCreateOrderForTable(Integer tableNumber) {
        try {
            logger.info("Getting or creating order for table: {}", tableNumber);
            
            // Validate table number with extended range for takeout
            if (tableNumber == null || tableNumber < 1 || tableNumber > 9999) {
                throw new IllegalArgumentException("Бројот на маса мора да биде помеѓу 1 и 9999");
            }

            // Check database health
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }

            // Try to find existing active order
            Optional<Order> existingOrder = findActiveOrderForTable(tableNumber);
            
            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                logger.info("Found existing order for table {}: ID {}, Status {}", 
                    tableNumber, order.getId(), order.getStatus());
                return order;
            }

            // Create new order
            logger.info("Creating new order for table {} ({})", 
                tableNumber, tableNumber >= 1000 ? "takeout" : "dine-in");
            
            Order newOrder = new Order(tableNumber);
            newOrder.setStatus(Order.Status.ОТВОРЕНА);
            newOrder.setCreatedAt(LocalDateTime.now());
            newOrder.setUpdatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(newOrder);
            orderRepository.flush(); // Ensure immediate persistence
            
            logger.info("Successfully created new order for table {}: ID {}", 
                tableNumber, savedOrder.getId());
            
            return savedOrder;

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid table number: {}", tableNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Error getting/creating order for table: {}", tableNumber, e);
            throw new RuntimeException("Грешка при креирање на нарачката: " + e.getMessage());
        }
    }


 // Add this method to OrderService.java

/**
 * Move order from one table to another
 * Simple implementation - just changes the table number
 */
public Order moveOrderToTable(Long orderId, Integer newTableNumber) {
    try {
        logger.info("Moving order {} to table {}", orderId, newTableNumber);
        
        // Validate new table number
        if (newTableNumber == null || newTableNumber < 1 || newTableNumber > 9999) {
            throw new IllegalArgumentException("Бројот на нова маса мора да биде помеѓу 1 и 9999");
        }

        // Get the order to move
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        // Check if order can be moved
        if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
            throw new RuntimeException("Не можете да преместите затворена нарачка");
        }

        Integer oldTableNumber = order.getTableNumber();

        // Check if target table already has an active order
        Optional<Order> existingOrder = findActiveOrderForTable(newTableNumber);
        if (existingOrder.isPresent() && !existingOrder.get().getId().equals(orderId)) {
            throw new RuntimeException("Масата " + newTableNumber + " веќе има активна нарачка");
        }

        // Move the order
        order.setTableNumber(newTableNumber);
        order.setUpdatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        orderRepository.flush();
        
        logger.info("Successfully moved order {} from table {} to table {}", 
            orderId, oldTableNumber, newTableNumber);
        
        return savedOrder;

    } catch (RuntimeException e) {
        logger.error("Error moving order {}: {}", orderId, e.getMessage());
        throw e;
    } catch (Exception e) {
        logger.error("Unexpected error moving order {}", orderId, e);
        throw new RuntimeException("Грешка при преместување на нарачката: " + e.getMessage());
    }
}

    public Order addItemToOrder(Long orderId, Long menuItemId, Integer quantity, String notes) {
        try {
            logger.info("Adding item {} (qty: {}) to order {}", menuItemId, quantity, orderId);
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            MenuItem menuItem = menuItemService.getItemById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

            if (!menuItem.getAvailable()) {
                throw new RuntimeException("Производот не е достапен");
            }

            // Check for existing item
            Optional<OrderItem> existingItem = order.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(menuItemId) && 
                               (notes == null ? item.getNotes() == null : notes.equals(item.getNotes())))
                .findFirst();

            if (existingItem.isPresent()) {
                // Update quantity of existing item
                OrderItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                item.calculateTotalPrice();
                orderItemRepository.save(item);
                logger.info("Updated existing item quantity to {}", item.getQuantity());
            } else {
                // Create new item
                OrderItem newItem = new OrderItem(order, menuItem, quantity, notes);
                order.getItems().add(newItem);
                orderItemRepository.save(newItem);
                logger.info("Added new item to order");
            }

            // Update order totals and status
            order.calculateTotal();
            order.setUpdatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            
            logger.info("Successfully added item to order {}. New total: {}", 
                orderId, savedOrder.getTotalAmount());
            
            return savedOrder;

        } catch (RuntimeException e) {
            logger.error("Runtime error adding item to order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error adding item to order {}", orderId, e);
            throw new RuntimeException("Грешка при додавање на производот");
        }
    }

    public Order removeItemFromOrder(Long orderId, Long itemId) {
        try {
            logger.info("Removing item {} from order {}", itemId, orderId);
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            OrderItem itemToRemove = order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден во нарачката"));

            // Check if item has sent quantity
            if (itemToRemove.hasSentQuantity()) {
                throw new RuntimeException("Не можете да го отстраните производот кој е веќе испратен. Потребен е админ код.");
            }

            order.getItems().remove(itemToRemove);
            orderItemRepository.delete(itemToRemove);

            order.calculateTotal();
            order.setUpdatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            
            logger.info("Successfully removed item from order {}. New total: {}", 
                orderId, savedOrder.getTotalAmount());
            
            return savedOrder;

        } catch (RuntimeException e) {
            logger.error("Runtime error removing item from order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error removing item from order {}", orderId, e);
            throw new RuntimeException("Грешка при отстранување на производот");
        }
    }

    public Order removeItemFromOrderWithAdmin(Long orderId, Long itemId) {
        try {
            logger.info("Admin removing item {} from order {}", itemId, orderId);
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            OrderItem itemToRemove = order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден во нарачката"));

            // Admin can remove any item, even if sent
            order.getItems().remove(itemToRemove);
            orderItemRepository.delete(itemToRemove);

            order.calculateTotal();
            order.setUpdatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            
            logger.info("Admin successfully removed item from order {}. New total: {}", 
                orderId, savedOrder.getTotalAmount());
            
            return savedOrder;

        } catch (RuntimeException e) {
            logger.error("Runtime error admin removing item from order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error admin removing item from order {}", orderId, e);
            throw new RuntimeException("Грешка при отстранување на производот");
        }
    }

    public Order updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        try {
            logger.info("Updating item {} quantity to {} in order {}", itemId, newQuantity, orderId);
            
            if (newQuantity == null || newQuantity < 1) {
                throw new RuntimeException("Количината мора да биде најмалку 1");
            }

            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            OrderItem item = order.getItems().stream()
                .filter(orderItem -> orderItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден во нарачката"));

            // Check if trying to reduce below sent quantity
            if (item.hasSentQuantity() && newQuantity < item.getSentQuantity()) {
                throw new RuntimeException(String.format(
                    "Не можете да ја намалите количината под веќе испратената (%d парчиња). Можете само да додадете повеќе.",
                    item.getSentQuantity()));
            }

            item.setQuantity(newQuantity);
            item.calculateTotalPrice();
            orderItemRepository.save(item);

            order.calculateTotal();
            order.setUpdatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            
            logger.info("Successfully updated item quantity in order {}. New total: {}", 
                orderId, savedOrder.getTotalAmount());
            
            return savedOrder;

        } catch (RuntimeException e) {
            logger.error("Runtime error updating item quantity in order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating item quantity in order {}", orderId, e);
            throw new RuntimeException("Грешка при ажурирање на количината");
        }
    }

    public void sendOrder(Long orderId) {
        try {
            logger.info("Sending order {} to kitchen/bar", orderId);
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

            if (order.getItems().isEmpty()) {
                throw new RuntimeException("Не можете да испратите празна нарачка");
            }

            if (order.getStatus() == Order.Status.ИСПРАТЕНА) {
                throw new RuntimeException("Оваа нарачка е веќе испратена");
            }

            if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
                throw new RuntimeException("Не можете да испратите затворена нарачка");
            }

            // Mark items as sent
            for (OrderItem item : order.getItems()) {
                if (item.getPendingQuantity() > 0) {
                    item.markAllAsSent();
                    orderItemRepository.save(item);
                }
            }

            // Update order status
            order.setStatus(Order.Status.ИСПРАТЕНА);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            orderRepository.flush();

            // Print tickets to kitchen/bar
            try {
                printerService.printOrderTickets(order);
                logger.info("Order tickets printed for order {}", orderId);
            } catch (Exception e) {
                logger.warn("Failed to print order tickets for order {}: {}", orderId, e.getMessage());
                // Don't fail the whole operation if printing fails
            }

            logger.info("Successfully sent order {} to kitchen/bar", orderId);

        } catch (RuntimeException e) {
            logger.error("Runtime error sending order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending order {}", orderId, e);
            throw new RuntimeException("Грешка при испраќање на нарачката");
        }
    }

    /**
     * Close order and print receipt
     * ENHANCED: Support for both thermal and fiscal printers
     */
    public void closeOrder(Long orderId, boolean useFiscalPrinter) {
        logger.info("Closing order {} with {} printer", orderId, useFiscalPrinter ? "fiscal" : "thermal");
        
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
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            orderRepository.flush();
            
            logger.info("Order {} status changed to CLOSED", orderId);

            // Print receipt based on printer type
            try {
                if (useFiscalPrinter) {
                    logger.info("Printing fiscal receipt for order {}", orderId);
                    printerService.printFiscalReceipt(order);
                } else {
                    logger.info("Printing thermal receipt for order {}", orderId);
                    printerService.printReceipt(order);
                }
            } catch (Exception e) {
                logger.warn("Failed to print receipt for order {}: {}", orderId, e.getMessage());
                // Don't fail the whole operation if printing fails
            }
            
            logger.info("Successfully closed order {}", orderId);
            
        } catch (RuntimeException e) {
            logger.error("Runtime error closing order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error closing order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Грешка при затворање на нарачката: " + e.getMessage());
        }
    }

    /**
     * Close order with default thermal printer (for backward compatibility)
     */
    public void closeOrder(Long orderId) {
        closeOrder(orderId, false);
    }
}