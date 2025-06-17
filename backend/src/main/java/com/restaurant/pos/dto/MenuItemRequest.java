package com.restaurant.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class MenuItemRequest {
    @NotBlank(message = "Името е задолжително")
    private String name;

    @NotNull(message = "Цената е задолжителна")
    @Positive(message = "Цената мора да биде позитивна")
    private BigDecimal price;

    @NotNull(message = "Категоријата е задолжителна")
    private String category;

    @NotNull(message = "Дестинацијата за печатење е задолжителна")
    private String printDestination;

    // Constructors
    public MenuItemRequest() { }

    public MenuItemRequest(String name, BigDecimal price, String category, String printDestination) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.printDestination = printDestination;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPrintDestination() { return printDestination; }
    public void setPrintDestination(String printDestination) { this.printDestination = printDestination; }
}