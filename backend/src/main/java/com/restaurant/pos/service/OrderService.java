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
     * Get or create order for table - FIXED to handle sent orders properly
     */
    public Order getOrCreateOrderForTable(Integer tableNumber) {
        logger.info("Getting or creating order for table: {}", tableNumber);
        
        // First, look for an OPEN order (can still add items)
        Optional<Order> openOrder = orderRepository.findByTableNumberAndStatus(
            tableNumber, Order.Status.ОТВОРЕНА);
        
        if (openOrder.isPresent()) {
            logger.info("Found existing open order for table {}: {}", tableNumber, openOrder.get().getId());
            return openOrder.get();
        }
        
        // Check if there's a SENT order (should not add items to this)
        Optional<Order> sentOrder = orderRepository.findByTableNumberAndStatus(
            tableNumber, Order.Status.ИСПРАТЕНА);
        
        if (sentOrder.isPresent()) {
            logger.info("Found sent order for table {}, creating new order", tableNumber);
            // Create a new order instead of returning the sent one
            Order newOrder = new Order(tableNumber);
            Order savedOrder = orderRepository.save(newOrder);
            logger.info("Created new order {} for table {} (previous order {} was sent)", 
                savedOrder.getId(), tableNumber, sentOrder.get().getId());
            return savedOrder;
        }
        
        // No existing order, create new one
        logger.info("No existing order for table {}, creating new order", tableNumber);
        Order newOrder = new Order(tableNumber);
        Order savedOrder = orderRepository.save(newOrder);
        logger.info("Created new order {} for table {}", savedOrder.getId(), tableNumber);
        return savedOrder;
    }

    /**
     * Add item to order - FIXED to prevent adding to sent orders
     */
    public Order addItemToOrder(Long orderId, OrderItemRequest request) {
        logger.info("Adding item to order {}: menuItem={}, quantity={}", 
            orderId, request.getMenuItemId(), request.getQuantity());
            
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));
        
        // CRITICAL FIX: Prevent adding items to sent orders
        if (order.getStatus() == Order.Status.ИСПРАТЕНА) {
            throw new RuntimeException("Не можете да додавате производи во веќе испратена нарачка. Започнете нова нарачка.");
        }
        
        if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
            throw new RuntimeException("Не можете да додавате производи во затворена нарачка.");
        }
        
        MenuItem menuItem = menuItemService.getItemById(request.getMenuItemId())
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

        if (!menuItem.getAvailable()) {
            throw new RuntimeException("Производот не е достапен");
        }

        // Check if item with same notes already exists in this OPEN order
        Optional<OrderItem> existingItem = order.getItems().stream()
            .filter(item -> item.getMenuItem().getId().equals(request.getMenuItemId())
                && java.util.Objects.equals(item.getNotes(), request.getNotes()))
            .findFirst();

        if (existingItem.isPresent()) {
            // Update existing item quantity
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            orderItemRepository.save(item);
            logger.info("Updated existing item {} quantity to {}", item.getId(), item.getQuantity());
        } else {
            // Create new item
            OrderItem newItem = new OrderItem(order, menuItem, request.getQuantity(), request.getNotes());
            order.getItems().add(newItem);
            orderItemRepository.save(newItem);
            logger.info("Added new item {} to order {}", newItem.getId(), orderId);
        }

        order.calculateTotal();
        Order savedOrder = orderRepository.save(order);
        logger.info("Updated order {} total to {}", savedOrder.getId(), savedOrder.getTotalAmount());
        
        return savedOrder;
    }

    /**
     * Remove item from order - FIXED to prevent modifying sent orders
     */
    public Order removeItemFromOrder(Long orderId, Long itemId) {
        logger.info("Removing item {} from order {}", itemId, orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));
        
        // CRITICAL FIX: Prevent modifying sent orders
        if (order.getStatus() == Order.Status.ИСПРАТЕНА) {
            throw new RuntimeException("Не можете да менувате веќе испратена нарачка.");
        }
        
        if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
            throw new RuntimeException("Не можете да менувате затворена нарачка.");
        }
        
        OrderItem itemToRemove = order.getItems().stream()
            .filter(item -> item.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден во нарачката"));

        order.getItems().remove(itemToRemove);
        orderItemRepository.delete(itemToRemove);
        logger.info("Removed item {} from order {}", itemId, orderId);

        order.calculateTotal();
        Order savedOrder = orderRepository.save(order);
        logger.info("Updated order {} total after removal to {}", savedOrder.getId(), savedOrder.getTotalAmount());
        
        return savedOrder;
    }

    /**
     * Update item quantity - FIXED to prevent modifying sent orders
     */
    public Order updateItemQuantity(Long orderId, Long itemId, Integer newQuantity) {
        logger.info("Updating item {} quantity to {} in order {}", itemId, newQuantity, orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        // CRITICAL FIX: Prevent modifying sent orders
        if (order.getStatus() == Order.Status.ИСПРАТЕНА) {
            throw new RuntimeException("Не можете да менувате веќе испратена нарачка.");
        }
        
        if (order.getStatus() == Order.Status.ЗАТВОРЕНА) {
            throw new RuntimeException("Не можете да менувате затворена нарачка.");
        }

        if (newQuantity <= 0) {
            return removeItemFromOrder(orderId, itemId);
        }
        
        OrderItem item = order.getItems().stream()
            .filter(oi -> oi.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

        item.setQuantity(newQuantity);
        orderItemRepository.save(item);
        logger.info("Updated item {} quantity to {}", itemId, newQuantity);

        order.calculateTotal();
        Order savedOrder = orderRepository.save(order);
        logger.info("Updated order {} total after quantity change to {}", savedOrder.getId(), savedOrder.getTotalAmount());
        
        return savedOrder;
    }

    /**
     * Send order to kitchen/bar - Only allow sending OPEN orders
     */
    public Order sendOrderToKitchen(Long orderId) {
        logger.info("Sending order {} to kitchen", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        if (order.getStatus() != Order.Status.ОТВОРЕНА) {
            throw new RuntimeException("Можете да испраќате само отворени нарачки");
        }

        if (order.getItems().isEmpty()) {
            throw new RuntimeException("Не можете да испратите празна нарачка");
        }

        order.setStatus(Order.Status.ИСПРАТЕНА);
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} status changed to SENT", savedOrder.getId());

        // Print tickets
        printerService.printOrderTickets(savedOrder);

        return savedOrder;
    }

    /**
     * Close order and print receipt - Only allow closing SENT orders
     */
    public void closeOrder(Long orderId) {
        logger.info("Closing order {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Нарачката не е пронајдена"));

        if (order.getStatus() != Order.Status.ИСПРАТЕНА) {
            throw new RuntimeException("Можете да затворате само испратени нарачки");
        }

        order.setStatus(Order.Status.ЗАТВОРЕНА);
        orderRepository.save(order);
        logger.info("Order {} status changed to CLOSED", orderId);

        // Print receipt
        printerService.printReceipt(order);
    }

    /**
     * Get current active order for table (only OPEN orders)
     */
    public Optional<Order> getCurrentActiveOrderForTable(Integer tableNumber) {
        return orderRepository.findByTableNumberAndStatus(tableNumber, Order.Status.ОТВОРЕНА);
    }

    /**
     * Get sent order for table (for closing)
     */
    public Optional<Order> getSentOrderForTable(Integer tableNumber) {
        return orderRepository.findByTableNumberAndStatus(tableNumber, Order.Status.ИСПРАТЕНА);
    }

    /**
     * Check if table has any active orders (OPEN or SENT)
     */
    public boolean tableHasActiveOrder(Integer tableNumber) {
        Optional<Order> openOrder = orderRepository.findByTableNumberAndStatus(
            tableNumber, Order.Status.ОТВОРЕНА);
        Optional<Order> sentOrder = orderRepository.findByTableNumberAndStatus(
            tableNumber, Order.Status.ИСПРАТЕНА);
        
        return openOrder.isPresent() || sentOrder.isPresent();
    }

    /**
     * Get table status based on orders
     */
    public String getTableStatus(Integer tableNumber) {
        Optional<Order> openOrder = getCurrentActiveOrderForTable(tableNumber);
        if (openOrder.isPresent()) {
            return "OCCUPIED"; // Table has open order
        }
        
        Optional<Order> sentOrder = getSentOrderForTable(tableNumber);
        if (sentOrder.isPresent()) {
            return "SENT"; // Table has sent order (ready for payment)
        }
        
        return "AVAILABLE"; // No active orders
    }
}