package com.restaurant.pos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_tables", 
       indexes = {
           @Index(name = "idx_table_number_active", columnList = "table_number, active"),
           @Index(name = "idx_area_id_active", columnList = "area_id, active")
       })
public class RestaurantTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Бројот на маса е задолжителен")
    @Column(nullable = false)
    private Integer tableNumber;

    @NotNull(message = "Капацитетот е задолжителен")
    @Positive(message = "Капацитетот мора да биде позитивен")
    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableShape shape = TableShape.RECTANGLE;

    @Column(nullable = false)
    private Integer positionX = 0;

    @Column(nullable = false)
    private Integer positionY = 0;

    @Column(nullable = false)
    private Integer width = 80;

    @Column(nullable = false)
    private Integer height = 80;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    @JsonIgnore
    private Area area;

    // FIXED: Remove the problematic mapping and use a different approach
    @Transient
    private Order currentOrder;

    // Constructors
    public RestaurantTable() {}

    public RestaurantTable(Integer tableNumber, Integer capacity, Area area) {
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.area = area;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getTableNumber() { return tableNumber; }
    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }

    public TableShape getShape() { return shape; }
    public void setShape(TableShape shape) { this.shape = shape; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public Order getCurrentOrder() { return currentOrder; }
    public void setCurrentOrder(Order currentOrder) { this.currentOrder = currentOrder; }

    // Business methods
    public boolean isAvailable() {
        return status == TableStatus.AVAILABLE && active;
    }

    public boolean isOccupied() {
        return status == TableStatus.OCCUPIED || currentOrder != null;
    }

    public String getAreaName() {
        return area != null ? area.getName() : "Непозната област";
    }

    public enum TableStatus {
        AVAILABLE("Достапна"),
        OCCUPIED("Зафатена"),
        RESERVED("Резервирана"),
        CLEANING("Се чисти"),
        OUT_OF_ORDER("Неисправна");
        
        private final String displayName;

        TableStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TableShape {
        RECTANGLE("Правоаголна"),
        CIRCLE("Кружна"),
        SQUARE("Квадратна");
        
        private final String displayName;

        TableShape(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}