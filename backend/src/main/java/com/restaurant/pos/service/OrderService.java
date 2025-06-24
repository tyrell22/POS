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

    // ... (keep all existing methods unchanged until closeOrder)

    /**
     * Close order and print receipt
     * NEW: Support for fiscal receipt when useFiscalPrinter is true
     */
    public void closeOrder(Long orderId, boolean useFiscalPrinter) {
        logger.info("Closing order {} with fiscal printer: {}", orderId, useFiscalPrinter);
        
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
     * Close order with default thermal printer (for backward compatibility)
     */
    public void closeOrder(Long orderId) {
        closeOrder(orderId, false); // Default to thermal printer
    }

    // ... (keep all other existing methods unchanged)
}