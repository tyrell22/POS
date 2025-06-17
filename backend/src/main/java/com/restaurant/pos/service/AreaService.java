package com.restaurant.pos.service;

import com.restaurant.pos.entity.Area;
import com.restaurant.pos.entity.FloorPlan;
import com.restaurant.pos.entity.RestaurantTable;
import com.restaurant.pos.repository.AreaRepository;
import com.restaurant.pos.repository.FloorPlanRepository;
import com.restaurant.pos.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    public List<Area> getAllActiveAreas() {
        return areaRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Area> getAreasByFloorPlan(Long floorPlanId) {
        return areaRepository.findByFloorPlanIdWithTables(floorPlanId);
    }

    public Optional<Area> getAreaById(Long id) {
        return areaRepository.findByIdWithTables(id);
    }

    public Area createArea(Long floorPlanId, String name, String description, Area.AreaType type, String color) {
        FloorPlan floorPlan = floorPlanRepository.findById(floorPlanId)
            .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

        if (areaRepository.existsByNameAndFloorPlanId(name, floorPlanId)) {
            throw new RuntimeException("Областа со ова име веќе постои во овој план");
        }
        
        Area area = new Area(name, type, floorPlan);
        area.setDescription(description);
        area.setColor(color != null ? color : getDefaultColorForType(type));

        // Set default position (find empty space)
        setDefaultPosition(area, floorPlanId);

        return areaRepository.save(area);
    }

    public Area updateArea(Long id, String name, String description, Area.AreaType type, String color) {
        Area area = areaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

        // Check if name is unique within the floor plan (excluding current area)
        if (!area.getName().equals(name) &&
            areaRepository.existsByNameAndFloorPlanId(name, area.getFloorPlan().getId())) {
            throw new RuntimeException("Областа со ова име веќе постои во овој план");
        }

        area.setName(name);
        area.setDescription(description);
        area.setType(type);
        area.setColor(color != null ? color : getDefaultColorForType(type));

        return areaRepository.save(area);
    }

    public Area updateAreaPosition(Long id, Integer x, Integer y, Integer width, Integer height) {
        Area area = areaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

        area.setPositionX(x);
        area.setPositionY(y);
        area.setWidth(width);
        area.setHeight(height);

        return areaRepository.save(area);
    }

    public void deleteArea(Long id) {
        Area area = areaRepository.findByIdWithTables(id)
            .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

        if (!area.getTables().isEmpty()) {
            throw new RuntimeException("Не можете да ја избришете областа која содржи маси. Прво преместете ги масите.");
        }

        area.setActive(false);
        areaRepository.save(area);
    }

    public void deleteAreaForce(Long id) {
        Area area = areaRepository.findByIdWithTables(id)
            .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

        // Deactivate all tables in this area
        for (RestaurantTable table : area.getTables()) {
            table.setActive(false);
            tableRepository.save(table);
        }

        area.setActive(false);
        areaRepository.save(area);
    }

    public Area addTableToArea(Long areaId, Integer tableNumber, Integer capacity, Integer x, Integer y) {
        Area area = areaRepository.findByIdWithTables(areaId)
            .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

        if (tableRepository.existsByTableNumberAndActiveTrue(tableNumber)) {
            throw new RuntimeException("Масата со овој број веќе постои");
        }
        
        RestaurantTable table = new RestaurantTable(tableNumber, capacity, area);
        table.setPositionX(x != null ? x : 20);
        table.setPositionY(y != null ? y : 20);
        table.setShape(RestaurantTable.TableShape.RECTANGLE);

        tableRepository.save(table);

        return areaRepository.findByIdWithTables(areaId).orElse(area);
    }

    public Long getTableCountForArea(Long areaId) {
        return tableRepository.countByAreaId(areaId);
    }

    private String getDefaultColorForType(Area.AreaType type) {
        return switch (type) {
            case DINING -> "#3B82F6";      // Blue
            case BAR -> "#8B5CF6";         // Purple
            case TERRACE -> "#10B981";     // Green
            case VIP -> "#F59E0B";         // Amber
            case PRIVATE_ROOM -> "#EF4444"; // Red
            case OUTDOOR -> "#059669";     // Emerald
            case SMOKING -> "#6B7280";     // Gray
            case NON_SMOKING -> "#14B8A6"; // Teal
        };
    }

    private void setDefaultPosition(Area area, Long floorPlanId) {
        List < Area > existingAreas = areaRepository.findByFloorPlanIdAndActiveTrueOrderByNameAsc(floorPlanId);

        if (existingAreas.isEmpty()) {
            // First area - place at top-left
            area.setPositionX(50);
            area.setPositionY(50);
        } else {
            // Find the rightmost area and place new area to its right
            int maxX = existingAreas.stream()
                .mapToInt(a -> a.getPositionX() + a.getWidth())
                .max()
                .orElse(50);

            area.setPositionX(maxX + 20);
            area.setPositionY(50);
        }

        // Set default size
        area.setWidth(200);
        area.setHeight(150);
    }
}