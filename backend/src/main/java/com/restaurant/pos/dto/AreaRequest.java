package com.restaurant.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AreaRequest {
    @NotNull(message = "ID на план е задолжително")
    private Long floorPlanId;

    @NotBlank(message = "Името на областа е задолжително")
    private String name;

    private String description;

    @NotBlank(message = "Типот на област е задолжителен")
    private String type;

    private String color;

    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;

    // Constructors
    public AreaRequest() { }

    public AreaRequest(Long floorPlanId, String name, String type) {
        this.floorPlanId = floorPlanId;
        this.name = name;
        this.type = type;
    }

    // Getters and Setters
    public Long getFloorPlanId() { return floorPlanId; }
    public void setFloorPlanId(Long floorPlanId) { this.floorPlanId = floorPlanId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}