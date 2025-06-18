package com.restaurant.pos.dto;

import jakarta.validation.constraints.NotBlank;

public class FloorPlanRequest {
    @NotBlank(message = "Името на планот е задолжително")
    private String name;

    private String description;

    // Constructors
    public FloorPlanRequest() { }

    public FloorPlanRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}