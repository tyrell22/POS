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
    private DatabaseHealthService databaseHealthService;
    
    @Autowired
    private TableService tableService;

    @Transactional(readOnly = true)
    public List<Area> getAllActiveAreas() {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            return areaRepository.findByActiveTrueOrderByNameAsc();
        } catch (DataAccessException e) {
            logger.error("Database error getting active areas", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public List<Area> getAreasByFloorPlan(Long floorPlanId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            return areaRepository.findByFloorPlanIdWithTables(floorPlanId);
        } catch (DataAccessException e) {
            logger.error("Database error getting areas by floor plan: {}", floorPlanId, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Area> getAreaById(Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            return areaRepository.findByIdWithTables(id);
        } catch (DataAccessException e) {
            logger.error("Database error getting area by ID: {}", id, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    /**
     * Add table to area with simplified grid positioning
     * UPDATED: Enhanced to use new table number reuse logic
     */
    public Area addTableToArea(Long areaId, Map<String, Object> tableData) {
        try {
            logger.info("Adding table to area {} with simplified grid layout", areaId);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            if (areaId == null) {
                throw new RuntimeException("ID на област е задолжително");
            }

            Area area = areaRepository.findByIdWithTables(areaId)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            // Extract table data
            Integer tableNumber = extractInteger(tableData, "tableNumber");
            Integer capacity = extractInteger(tableData, "capacity");

            // If no table number provided, find the next available one
            if (tableNumber == null || tableNumber <= 0) {
                tableNumber = tableService.getNextTableNumber();
                logger.info("No table number provided, using next available: {}", tableNumber);
            }

            // Validate required fields
            if (capacity == null || capacity <= 0) {
                throw new RuntimeException("Капацитетот е задолжителен и мора да биде позитивен");
            }

            // UPDATED: Check if table number is available (only among active tables)
            if (!tableService.isTableNumberAvailable(tableNumber)) {
                // Try to find the next available number
                tableNumber = tableService.getNextTableNumber();
                logger.info("Requested table number not available, using: {}", tableNumber);
                
                // Double-check the new number
                if (!tableService.isTableNumberAvailable(tableNumber)) {
                    throw new RuntimeException("Не можам да најдам достапен број на маса");
                }
            }
            
            // Create new table with simplified grid positioning
            RestaurantTable table = new RestaurantTable(tableNumber, capacity, area);
            
            // Calculate grid position based on existing active tables count
            List<RestaurantTable> activeTables = area.getTables() != null 
                ? area.getTables().stream().filter(RestaurantTable::getActive).toList()
                : List.of();
            
            int existingTablesCount = activeTables.size();
            int gridRow = existingTablesCount / 2;  // 2 columns
            int gridCol = existingTablesCount % 2;
            
            // Set simplified grid position
            table.setPositionX(gridCol * 150);  // Column spacing
            table.setPositionY(gridRow * 100);  // Row spacing
            table.setWidth(120);
            table.setHeight(80);
            table.setShape(RestaurantTable.TableShape.RECTANGLE);
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);

            // Save the table
            RestaurantTable savedTable = tableRepository.save(table);
            tableRepository.flush();
            
            logger.info("Successfully created table {} (ID: {}) in simplified grid position ({},{})", 
                tableNumber, savedTable.getId(), gridCol, gridRow);

            // Return the updated area
            Area updatedArea = areaRepository.findByIdWithTables(areaId).orElse(area);
            
            return updatedArea;

        } catch (DataAccessException e) {
            logger.error("Database error adding table to area: {}", areaId, e);
            throw new RuntimeException("Грешка при додавање на масата: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error adding table to area: {}", areaId, e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Неочекувана грешка при додавање на масата: " + e.getMessage());
        }
    }

    /**
     * Remove table and reorganize grid
     */
    public void removeTableFromArea(Long areaId, Long tableId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }

            Area area = areaRepository.findByIdWithTables(areaId)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

            if (!table.getArea().getId().equals(areaId)) {
                throw new RuntimeException("Масата не припаѓа на оваа област");
            }

            // Soft delete - this now allows the table number to be reused
            table.setActive(false);
            tableRepository.save(table);
            tableRepository.flush();
            
            // Reorganize remaining active tables in grid
            reorganizeTableGrid(area);
            
            logger.info("Soft deleted table {} from area: {} and reorganized grid. Table number {} is now available for reuse.", 
                table.getTableNumber(), area.getName(), table.getTableNumber());

        } catch (DataAccessException e) {
            logger.error("Database error removing table from area: {}", areaId, e);
            throw new RuntimeException("Грешка при отстранување на масата");
        }
    }

    /**
     * Delete table by ID and reorganize grid
     */
    public void deleteTable(Long tableId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }

            RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

            Area area = table.getArea();
            Integer tableNumber = table.getTableNumber();
            
            // Soft delete the table - this allows table number reuse
            table.setActive(false);
            tableRepository.save(table);
            tableRepository.flush();
            
            // Reorganize remaining active tables in grid
            reorganizeTableGrid(area);
            
            logger.info("Soft deleted table {} and reorganized grid in area {}. Table number {} is now available for reuse.", 
                tableNumber, area.getName(), tableNumber);

        } catch (DataAccessException e) {
            logger.error("Database error deleting table: {}", tableId, e);
            throw new RuntimeException("Грешка при бришење на масата");
        }
    }

    /**
     * Reorganize tables in simplified 2-column grid after deletion
     * UPDATED: Only reorganize active tables
     */
    private void reorganizeTableGrid(Area area) {
        try {
            // Get all active tables in the area, sorted by table number
            List<RestaurantTable> activeTables = tableRepository.findByAreaIdAndActiveTrueOrderByTableNumberAsc(area.getId());
            
            // Reorganize active tables in 2-column grid
            for (int i = 0; i < activeTables.size(); i++) {
                RestaurantTable table = activeTables.get(i);
                
                int gridRow = i / 2;  // 2 columns
                int gridCol = i % 2;
                
                table.setPositionX(gridCol * 150);  // Column spacing
                table.setPositionY(gridRow * 100);  // Row spacing
                
                tableRepository.save(table);
            }
            
            tableRepository.flush();
            logger.info("Reorganized {} active tables in grid for area {}", activeTables.size(), area.getName());
            
        } catch (Exception e) {
            logger.error("Error reorganizing table grid for area {}: {}", area.getName(), e.getMessage());
        }
    }

    /**
     * Get next available table number
     * UPDATED: Uses the enhanced TableService logic
     */
    public Integer getNextTableNumber() {
        try {
            return tableService.getNextTableNumber();
        } catch (Exception e) {
            logger.error("Error getting next table number", e);
            return 1;
        }
    }

    // Helper method to safely extract integers from map
    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                String str = ((String) value).trim();
                if (str.isEmpty() || "undefined".equals(str) || "null".equals(str)) {
                    return null;
                }
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse integer from value: '{}' for key: '{}'", value, key);
        }
        
        return null;
    }

    @Transactional(readOnly = true)
    public Long getTableCountForArea(Long areaId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return 0L;
            }
            return tableRepository.countByAreaId(areaId);
        } catch (DataAccessException e) {
            logger.error("Database error counting tables for area: {}", areaId, e);
            return 0L;
        }
    }

    // Standard area management methods
    public Area createArea(Long floorPlanId, String name, String description, Area.AreaType type, String color) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }

            FloorPlan floorPlan = floorPlanRepository.findById(floorPlanId)
                .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

            if (areaRepository.existsByNameAndFloorPlanId(name, floorPlanId)) {
                throw new RuntimeException("Областа со ова име веќе постои во овој план");
            }
            
            Area area = new Area(name, type, floorPlan);
            area.setDescription(description);
            area.setColor(color != null ? color : getDefaultColorForType(type));
            area.setPositionX(0);
            area.setPositionY(0);
            area.setWidth(400);
            area.setHeight(300);

            Area savedArea = areaRepository.save(area);
            areaRepository.flush();
            
            logger.info("Created area: {} with ID: {}", savedArea.getName(), savedArea.getId());
            return savedArea;

        } catch (DataAccessException e) {
            logger.error("Database error creating area", e);
            throw new RuntimeException("Грешка при креирање на областа");
        }
    }

    public Area updateArea(Long id, String name, String description, Area.AreaType type, String color) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }

            Area area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            if (!area.getName().equals(name) &&
                areaRepository.existsByNameAndFloorPlanId(name, area.getFloorPlan().getId())) {
                throw new RuntimeException("Областа со ова име веќе постои во овој план");
            }

            area.setName(name);
            area.setDescription(description);
            area.setType(type);
            area.setColor(color != null ? color : getDefaultColorForType(type));

            Area savedArea = areaRepository.save(area);
            areaRepository.flush();
            
            logger.info("Updated area: {} with ID: {}", savedArea.getName(), savedArea.getId());
            return savedArea;

        } catch (DataAccessException e) {
            logger.error("Database error updating area: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на областа");
        }
    }

    public void deleteArea(Long id) {
        try {
            logger.info("Attempting to delete area with ID: {}", id);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            Area area = areaRepository.findByIdWithTables(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            // Check only active tables
            List<RestaurantTable> activeTables = area.getTables() != null 
                ? area.getTables().stream().filter(RestaurantTable::getActive).toList()
                : List.of();

            if (!activeTables.isEmpty()) {
                throw new RuntimeException("Не можете да ја избришете областа која содржи активни маси. Прво избришете ги масите.");
            }

            area.setActive(false);
            areaRepository.save(area);
            areaRepository.flush();
            
            logger.info("Successfully deleted area: {}", area.getName());

        } catch (Exception e) {
            logger.error("Error deleting area: {}", id, e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Грешка при бришење на областа: " + e.getMessage());
        }
    }

    public void deleteAreaForce(Long id) {
        try {
            logger.info("Attempting to force delete area with ID: {}", id);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }
            
            if (id == null) {
                throw new RuntimeException("ID на област е задолжително");
            }
            
            Area area = areaRepository.findByIdWithTables(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            logger.info("Found area: {} with {} tables", area.getName(), 
                area.getTables() != null ? area.getTables().size() : 0);

            // Soft delete all tables in this area (both active and inactive)
            if (area.getTables() != null && !area.getTables().isEmpty()) {
                logger.info("Deactivating {} tables in area", area.getTables().size());
                for (RestaurantTable table : area.getTables()) {
                    table.setActive(false);
                    tableRepository.save(table);
                }
                tableRepository.flush();
                logger.info("Deactivated all tables in area");
                
                // Clear the tables list to avoid constraint issues
                area.getTables().clear();
                areaRepository.save(area);
                areaRepository.flush();
            }

            // Then deactivate the area
            area.setActive(false);
            areaRepository.save(area);
            areaRepository.flush();
            
            logger.info("Successfully force deleted area: {} with all its tables", area.getName());

        } catch (DataAccessException e) {
            logger.error("Database error force deleting area: {}", id, e);
            throw new RuntimeException("Грешка при бришење на областа: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error force deleting area: {}", id, e);
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Неочекувана грешка: " + e.getMessage());
        }
    }

    public Area updateAreaPosition(Long id, Integer x, Integer y, Integer width, Integer height) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("База на податоци недостапна");
            }

            Area area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            if (x != null) area.setPositionX(x);
            if (y != null) area.setPositionY(y);
            if (width != null) area.setWidth(width);
            if (height != null) area.setHeight(height);

            Area savedArea = areaRepository.save(area);
            areaRepository.flush();
            
            logger.info("Updated position for area: {} with ID: {}", savedArea.getName(), savedArea.getId());
            return savedArea;

        } catch (DataAccessException e) {
            logger.error("Database error updating area position: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на позицијата");
        }
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
}