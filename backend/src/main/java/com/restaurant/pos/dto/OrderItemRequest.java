package com.restaurant.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OrderItemRequest {
    @NotNull(message = "ID на производ е задолжително")
    private Long menuItemId;

    @NotNull(message = "Количината е задолжителна")
    @Positive(message = "Количината мора да биде позитивна")
    private Integer quantity;

    private String notes;

    // Constructors
    public OrderItemRequest() { }

    public OrderItemRequest(Long menuItemId, Integer quantity, String notes) {
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}