package com.restaurant.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UPDATED: Extended range to support takeout orders (1000-9999)
    @Min(value = 1, message = "Бројот на маса мора да биде најмалку 1")
    @Max(value = 9999, message = "Бројот на маса мора да биде најмногу 9999")
    @Column(nullable = false)
    private Integer tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ОТВОРЕНА;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    // Constructors
    public Order() { }

    public Order(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void calculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if this is a takeout order
     * Takeout orders have table numbers >= 1000
     */
    public boolean isTakeoutOrder() {
        return tableNumber != null && tableNumber >= 1000;
    }

    /**
     * Get display name for the order
     */
    public String getDisplayName() {
        if (isTakeoutOrder()) {
            return "Понеси #" + (tableNumber - 1000 + 1);
        } else {
            return "Маса " + tableNumber;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getTableNumber() { return tableNumber; }
    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public enum Status {
        ОТВОРЕНА("Отворена"),
        ИСПРАТЕНА("Испратена"), 
        ЗАТВОРЕНА("Затворена");
        
        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}