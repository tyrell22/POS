package com.restaurant.pos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "areas")
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Името на областа е задолжително")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Типот на област е задолжителен")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AreaType type;

    @Column(nullable = false)
    private String color = "#3B82F6"; // Default blue

    @Column(nullable = false)
    private Integer positionX = 0;

    @Column(nullable = false)
    private Integer positionY = 0;

    @Column(nullable = false)
    private Integer width = 200;

    @Column(nullable = false)
    private Integer height = 150;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_plan_id", nullable = false)
    @JsonIgnore
    private FloorPlan floorPlan;

    @OneToMany(mappedBy = "area", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<RestaurantTable> tables = new ArrayList <> ();

    // Constructors
    public Area() { }

    public Area(String name, AreaType type, FloorPlan floorPlan) {
        this.name = name;
        this.type = type;
        this.floorPlan = floorPlan;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public AreaType getType() { return type; }
    public void setType(AreaType type) { this.type = type; }

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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public FloorPlan getFloorPlan() { return floorPlan; }
    public void setFloorPlan(FloorPlan floorPlan) { this.floorPlan = floorPlan; }

    public List<RestaurantTable> getTables() { return tables; }
    public void setTables(List<RestaurantTable> tables) { this.tables = tables; }

    // Business methods
    public void addTable(RestaurantTable table) {
    tables.add(table);
    table.setArea(this);
}
    
    public void removeTable(RestaurantTable table) {
    tables.remove(table);
    table.setArea(null);
}

public enum AreaType {
    DINING("Трпезарија"),
        BAR("Бар"),
        TERRACE("Тераса"),
        VIP("VIP"),
        PRIVATE_ROOM("Приватна соба"),
        OUTDOOR("Надворешна"),
        SMOKING("Пушачка"),
        NON_SMOKING("Непушачка");
        
        private final String displayName;

AreaType(String displayName) {
    this.displayName = displayName;
}
        
        public String getDisplayName() {
    return displayName;
}
    }
}