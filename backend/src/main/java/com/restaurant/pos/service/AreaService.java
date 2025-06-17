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
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AreaService {

    private static final Logger logger = LoggerFactory.getLogger(AreaService.class);

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private TableService tableService;

    @Transactional(readOnly = true)
    public List<Area> getAllActiveAreas() {
        try {
            return areaRepository.findByActiveTrueOrderByNameAsc();
        } catch (DataAccessException e) {
            logger.error("Database error getting active areas", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public List<Area> getAreasByFloorPlan(Long floorPlanId) {
        try {
            return areaRepository.findByFloorPlanIdWithTables(floorPlanId);
        } catch (DataAccessException e) {
            logger.error("Database error getting areas by floor plan: {}", floorPlanId, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Area> getAreaById(Long id) {
        try {
            return areaRepository.findByIdWithTables(id);
        } catch (DataAccessException e) {
            logger.error("Database error getting area by ID: {}", id, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    public Area createArea(Long floorPlanId, String name, String description, Area.AreaType type, String color) {
        try {
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

            Area savedArea = areaRepository.save(area);
            areaRepository.flush(); // Force immediate persistence
            
            logger.info("Created and persisted area: {} with ID: {} in floor plan: {}", 
                savedArea.getName(), savedArea.getId(), floorPlan.getName());
            return savedArea;

        } catch (DataAccessException e) {
            logger.error("Database error creating area", e);
            throw new RuntimeException("Грешка при креирање на областа");
        }
    }

    public Area updateArea(Long id, String name, String description, Area.AreaType type, String color) {
        try {
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

            Area savedArea = areaRepository.save(area);
            areaRepository.flush(); // Force immediate persistence
            
            logger.info("Updated and persisted area: {} with ID: {}", savedArea.getName(), savedArea.getId());
            return savedArea;

        } catch (DataAccessException e) {
            logger.error("Database error updating area: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на областа");
        }
    }

    public Area updateAreaPosition(Long id, Integer x, Integer y, Integer width, Integer height) {
        try {
            Area area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            if (x != null) area.setPositionX(x);
            if (y != null) area.setPositionY(y);
            if (width != null) area.setWidth(width);
            if (height != null) area.setHeight(height);

            Area savedArea = areaRepository.save(area);
            areaRepository.flush(); // Force immediate persistence
            
            logger.info("Updated position for area: {} with ID: {}", savedArea.getName(), savedArea.getId());
            return savedArea;

        } catch (DataAccessException e) {
            logger.error("Database error updating area position: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на позицијата");
        }
    }

    public void deleteArea(Long id) {
        try {
            logger.info("Attempting to delete area with ID: {}", id);
            
            Area area = areaRepository.findByIdWithTables(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            logger.info("Found area: {} with {} tables", area.getName(), area.getTables().size());

            if (!area.getTables().isEmpty()) {
                throw new RuntimeException("Не можете да ја избришете областа која содржи маси. Прво преместете ги масите.");
            }

            area.setActive(false);
            areaRepository.save(area);
            
            logger.info("Successfully deleted area: {}", area.getName());

        } catch (DataAccessException e) {
            logger.error("Database error deleting area: {}", id, e);
            throw new RuntimeException("Грешка при бришење на областа");
        }
    }

    public void deleteAreaForce(Long id) {
        try {
            logger.info("Attempting to force delete area with ID: {}", id);
            
            Area area = areaRepository.findByIdWithTables(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            logger.info("Found area: {} with {} tables", area.getName(), area.getTables().size());

            // Deactivate all tables in this area first
            if (area.getTables() != null && !area.getTables().isEmpty()) {
                for (RestaurantTable table : area.getTables()) {
                    table.setActive(false);
                    tableRepository.save(table);
                }
                logger.info("Deactivated {} tables in area", area.getTables().size());
            }

            // Then deactivate the area
            area.setActive(false);
            areaRepository.save(area);
            
            logger.info("Successfully force deleted area: {} with {} tables", area.getName(), area.getTables().size());

        } catch (DataAccessException e) {
            logger.error("Database error force deleting area: {}", id, e);
            throw new RuntimeException("Грешка при бришење на областа");
        }
    }

    // Table management methods
    public Area addTableToArea(Long areaId, Integer tableNumber, Integer capacity, Integer x, Integer y) {
        try {
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
            
            logger.info("Added table {} to area: {}", tableNumber, area.getName());

            return areaRepository.findByIdWithTables(areaId).orElse(area);

        } catch (DataAccessException e) {
            logger.error("Database error adding table to area: {}", areaId, e);
            throw new RuntimeException("Грешка при додавање на масата");
        }
    }

    public Area addTableToArea(Long areaId, Map<String, Object> tableData) {
        try {
            Area area = areaRepository.findByIdWithTables(areaId)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            logger.info("Adding table to area {} with data: {}", area.getName(), tableData);

            // Extract table data safely
            Integer tableNumber = extractInteger(tableData, "tableNumber");
            Integer capacity = extractInteger(tableData, "capacity");
            String shapeStr = (String) tableData.get("shape");
            Integer positionX = extractInteger(tableData, "positionX");
            Integer positionY = extractInteger(tableData, "positionY");
            Integer width = extractInteger(tableData, "width");
            Integer height = extractInteger(tableData, "height");

            // Validate required fields
            if (tableNumber == null) {
                throw new RuntimeException("Бројот на маса е задолжителен");
            }
            if (capacity == null) {
                throw new RuntimeException("Капацитетот е задолжителен");
            }

            // Check if table number already exists
            if (tableRepository.existsByTableNumberAndActiveTrue(tableNumber)) {
                throw new RuntimeException("Масата со овој број веќе постои");
            }
            
            // Create new table
            RestaurantTable table = new RestaurantTable(tableNumber, capacity, area);
            table.setPositionX(positionX != null ? positionX : 20);
            table.setPositionY(positionY != null ? positionY : 20);
            table.setWidth(width != null ? width : 80);
            table.setHeight(height != null ? height : 80);
            
            if (shapeStr != null) {
                try {
                    table.setShape(RestaurantTable.TableShape.valueOf(shapeStr));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid table shape: {}, using default", shapeStr);
                    table.setShape(RestaurantTable.TableShape.RECTANGLE);
                }
            } else {
                table.setShape(RestaurantTable.TableShape.RECTANGLE);
            }

            RestaurantTable savedTable = tableRepository.save(table);
            tableRepository.flush(); // Force immediate persistence
            
            logger.info("Added and persisted table {} with ID: {} to area: {}", 
                tableNumber, savedTable.getId(), area.getName());

            // Return updated area with tables
            return areaRepository.findByIdWithTables(areaId).orElse(area);

        } catch (DataAccessException e) {
            logger.error("Database error adding table to area: {}", areaId, e);
            throw new RuntimeException("Грешка при додавање на масата");
        }
    }

    // Helper method to safely extract integers
    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                String str = (String) value;
                if ("undefined".equals(str) || "null".equals(str) || str.trim().isEmpty()) {
                    return null;
                }
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse integer from value: {} for key: {}", value, key);
        }
        
        return null;
    }

    public void removeTableFromArea(Long areaId, Long tableId) {
        try {
            Area area = areaRepository.findByIdWithTables(areaId)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

            if (!table.getArea().getId().equals(areaId)) {
                throw new RuntimeException("Масата не припаѓа на оваа област");
            }

            table.setActive(false);
            tableRepository.save(table);
            
            logger.info("Removed table {} from area: {}", table.getTableNumber(), area.getName());

        } catch (DataAccessException e) {
            logger.error("Database error removing table from area: {}", areaId, e);
            throw new RuntimeException("Грешка при отстранување на масата");
        }
    }

    @Transactional(readOnly = true)
    public Long getTableCountForArea(Long areaId) {
        try {
            return tableRepository.countByAreaId(areaId);
        } catch (DataAccessException e) {
            logger.error("Database error counting tables for area: {}", areaId, e);
            return 0L;
        }
    }

    // Helper methods
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
        try {
            List<Area> existingAreas = areaRepository.findByFloorPlanIdAndActiveTrueOrderByNameAsc(floorPlanId);

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
        } catch (Exception e) {
            logger.warn("Error setting default position, using fallback", e);
            area.setPositionX(50);
            area.setPositionY(50);
            area.setWidth(200);
            area.setHeight(150);
        }
    }
}