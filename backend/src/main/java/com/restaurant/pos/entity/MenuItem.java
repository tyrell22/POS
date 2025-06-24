package com.restaurant.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Името е задолжително")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Цената е задолжителна")
    @Positive(message = "Цената мора да биде позитивна")
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrintDestination printDestination;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public MenuItem() { }

    public MenuItem(String name, BigDecimal price, Category category, PrintDestination printDestination) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.printDestination = printDestination;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public PrintDestination getPrintDestination() { return printDestination; }
    public void setPrintDestination(PrintDestination printDestination) { this.printDestination = printDestination; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum Category {
    ХРАНА("Храна"),
        ПИЈАЛОЦИ("Пијалоци"),
        АЛКОХОЛ("Алкохол"),
        ДЕСЕРТИ("Десерти");
        
        private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
        
        public String getDisplayName() {
        return displayName;
    }
}

public enum PrintDestination {
    КУЈНА("Кујна"),
        БАР("Бар");
        
        private final String displayName;

PrintDestination(String displayName) {
    this.displayName = displayName;
}
        
        public String getDisplayName() {
    return displayName;
}
    }
}