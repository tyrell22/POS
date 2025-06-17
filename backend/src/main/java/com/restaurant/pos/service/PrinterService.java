package com.restaurant.pos.service;

import com.restaurant.pos.entity.Order;
import com.restaurant.pos.entity.OrderItem;
import com.restaurant.pos.entity.MenuItem;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PrinterService {
    
    private static final String ADMIN_CODE = "1234";
    
    public void printOrderTickets(Order order) {
        try {
            // Separate items by print destination
            List<OrderItem> kitchenItems = order.getItems().stream()
                .filter(item -> item.getMenuItem().getPrintDestination() == MenuItem.PrintDestination.КУЈНА)
                .toList();
            
            List<OrderItem> barItems = order.getItems().stream()
                .filter(item -> item.getMenuItem().getPrintDestination() == MenuItem.PrintDestination.БАР)
                .toList();
            
            // Print kitchen ticket if there are kitchen items
            if (!kitchenItems.isEmpty()) {
                printKitchenTicket(order, kitchenItems);
            }
            
            // Print bar ticket if there are bar items
            if (!barItems.isEmpty()) {
                printBarTicket(order, barItems);
            }
            
        } catch (Exception e) {
            System.err.println("Грешка при печатење: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printKitchenTicket(Order order, List<OrderItem> items) {
        String ticketContent = formatKitchenTicket(order, items);
        System.out.println("=== КУЈНА БИЛЕТ ===");
        System.out.println(ticketContent);
        System.out.println("==================");
    }
    
    private void printBarTicket(Order order, List<OrderItem> items) {
        String ticketContent = formatBarTicket(order, items);
        System.out.println("=== БАР БИЛЕТ ===");
        System.out.println(ticketContent);
        System.out.println("=================");
    }
    
    private String formatKitchenTicket(Order order, List<OrderItem> items) {
        StringBuilder ticket = new StringBuilder();
        ticket.append("========== КУЈНА ==========\n");
        ticket.append("Маса: ").append(order.getTableNumber()).append("\n");
        ticket.append("Време: ").append(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt())).append("\n");
        ticket.append("===========================\n\n");
        
        for (OrderItem item : items) {
            ticket.append(item.getQuantity()).append("x ")
                  .append(item.getMenuItem().getName()).append("\n");
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                ticket.append("   Забелешка: ").append(item.getNotes()).append("\n");
            }
            ticket.append("\n");
        }
        
        ticket.append("===========================\n");
        return ticket.toString();
    }
    
    private String formatBarTicket(Order order, List<OrderItem> items) {
        StringBuilder ticket = new StringBuilder();
        ticket.append("=========== БАР ===========\n");
        ticket.append("Маса: ").append(order.getTableNumber()).append("\n");
        ticket.append("Време: ").append(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").format(order.getCreatedAt())).append("\n");
        ticket.append("===========================\n\n");
        
        for (OrderItem item : items) {
            ticket.append(item.getQuantity()).append("x ")
                  .append(item.getMenuItem().getName()).append("\n");
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                ticket.append("   Забелешка: ").append(item.getNotes()).append("\n");
            }
            ticket.append("\n");
        }
        
        ticket.append("===========================\n");
        return ticket.toString();
    }
    
    public boolean validateAdminCode(String code) {
        return ADMIN_CODE.equals(code);
    }
    
    public void printReceipt(Order order) {
        String receiptContent = formatReceipt(order);
        System.out.println("=== СМЕТКА ===");
        System.out.println(receiptContent);
        System.out.println("==============");
    }
    
    private String formatReceipt(Order order) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("======= РЕСТОРАН POS =======\n");
        receipt.append("Маса: ").append(order.getTableNumber()).append("\n");
        receipt.append("Датум: ").append(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(order.getCreatedAt())).append("\n");
        receipt.append("============================\n\n");
        
        for (OrderItem item : order.getItems()) {
            receipt.append(String.format("%dx %-15s %8.2f ден\n", 
                item.getQuantity(), 
                item.getMenuItem().getName(), 
                item.getTotalPrice()));
        }
        
        receipt.append("\n============================\n");
        receipt.append(String.format("ВКУПНО: %20.2f ден\n", order.getTotalAmount()));
        receipt.append("============================\n\n");
        receipt.append("    Ви благодариме!\n");
        receipt.append("============================\n");
        
        return receipt.toString();
    }
}