package com.restaurant.pos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private Integer quantity;

    // NEW: Track how much of this item was sent to kitchen/bar
    @Column(nullable = false)
    private Integer sentQuantity = 0;

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public OrderItem() { }

    public OrderItem(Order order, MenuItem menuItem, Integer quantity, String notes) {
        this.order = order;
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.sentQuantity = 0; // Initialize as not sent
        this.unitPrice = menuItem.getPrice();
        this.notes = notes;
        calculateTotalPrice();
    }

    // Business methods
    public void calculateTotalPrice() {
        this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    /**
     * Check if this item has any sent quantity
     */
    public boolean hasSentQuantity() {
        return sentQuantity != null && sentQuantity > 0;
    }

    /**
     * Get the pending (not sent) quantity
     */
    public Integer getPendingQuantity() {
        return quantity - (sentQuantity != null ? sentQuantity : 0);
    }

    /**
     * Mark quantity as sent
     */
    public void markQuantityAsSent(Integer quantityToSend) {
        if (quantityToSend == null || quantityToSend <= 0) {
            return;
        }
        
        int maxCanSend = getPendingQuantity();
        int actualSent = Math.min(quantityToSend, maxCanSend);
        
        this.sentQuantity = (this.sentQuantity != null ? this.sentQuantity : 0) + actualSent;
    }

    /**
     * Mark all current quantity as sent
     */
    public void markAllAsSent() {
        this.sentQuantity = this.quantity;
    }

    /**
     * Check if all quantity is sent
     */
    public boolean isFullySent() {
        return sentQuantity != null && sentQuantity.equals(quantity);
    }

    /**
     * Check if can reduce quantity (only if not sent)
     */
    public boolean canReduceQuantity() {
        return getPendingQuantity() > 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateTotalPrice();
    }

    public Integer getSentQuantity() { return sentQuantity; }
    public void setSentQuantity(Integer sentQuantity) { this.sentQuantity = sentQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}