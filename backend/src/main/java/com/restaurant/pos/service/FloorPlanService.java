package com.restaurant.pos.service;

import com.restaurant.pos.entity.FloorPlan;
import com.restaurant.pos.entity.Area;
import com.restaurant.pos.entity.RestaurantTable;
import com.restaurant.pos.repository.FloorPlanRepository;
import com.restaurant.pos.repository.AreaRepository;
import com.restaurant.pos.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FloorPlanService {

    private static final Logger logger = LoggerFactory.getLogger(FloorPlanService.class);

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Transactional(readOnly = true)
    public List<FloorPlan> getAllActiveFloorPlans() {
        try {
            List<FloorPlan> plans = floorPlanRepository.findByActiveTrueOrderByNameAsc();
            logger.info("Found {} active floor plans", plans.size());
            return plans;
        } catch (DataAccessException e) {
            logger.error("Database error getting active floor plans", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<FloorPlan> getActiveFloorPlan() {
        try {
            logger.info("Looking for active floor plan...");
            
            // Get active floor plan with areas
            Optional<FloorPlan> activeFloorPlan = floorPlanRepository.findActiveFloorPlanWithAreas();
            
            if (activeFloorPlan.isPresent()) {
                FloorPlan floorPlan = activeFloorPlan.get();
                logger.info("Found active floor plan: {} with {} areas", 
                    floorPlan.getName(), floorPlan.getAreas().size());
                
                // Manually load tables for each area to ensure they're fetched
                for (Area area : floorPlan.getAreas()) {
                    // Force fetch tables
                    List<RestaurantTable> tables = tableRepository.findByAreaIdAndActiveTrueOrderByTableNumberAsc(area.getId());
                    area.setTables(tables);
                    logger.info("Area '{}' has {} tables", area.getName(), tables.size());
                }
                
                return Optional.of(floorPlan);
            }
            
            logger.info("No active floor plan found");
            return Optional.empty();
            
        } catch (DataAccessException e) {
            logger.error("Database error getting active floor plan", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<FloorPlan> getFloorPlanById(Long id) {
        try {
            if (id == null) {
                return Optional.empty();
            }
            
            Optional<FloorPlan> floorPlanOpt = floorPlanRepository.findByIdWithAreas(id);
            
            if (floorPlanOpt.isPresent()) {
                FloorPlan floorPlan = floorPlanOpt.get();
                // Manually load tables for each area
                for (Area area : floorPlan.getAreas()) {
                    List<RestaurantTable> tables = tableRepository.findByAreaIdAndActiveTrueOrderByTableNumberAsc(area.getId());
                    area.setTables(tables);
                }
                logger.info("Loaded floor plan {} with {} areas", floorPlan.getName(), floorPlan.getAreas().size());
            }
            
            return floorPlanOpt;
        } catch (DataAccessException e) {
            logger.error("Database error getting floor plan by ID: {}", id, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    public FloorPlan createFloorPlan(String name, String description) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Името на планот е задолжително");
            }
            
            FloorPlan floorPlan = new FloorPlan(name.trim(), description != null ? description.trim() : null);
            FloorPlan savedPlan = floorPlanRepository.save(floorPlan);
            
            // Flush to ensure it's persisted immediately
            floorPlanRepository.flush();
            
            logger.info("Created and persisted floor plan: {} with ID: {}", savedPlan.getName(), savedPlan.getId());
            return savedPlan;
        } catch (DataAccessException e) {
            logger.error("Database error creating floor plan", e);
            throw new RuntimeException("Грешка при креирање на планот");
        }
    }

    public FloorPlan updateFloorPlan(Long id, String name, String description) {
        try {
            FloorPlan floorPlan = floorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Името на планот е задолжително");
            }

            floorPlan.setName(name.trim());
            floorPlan.setDescription(description != null ? description.trim() : null);

            FloorPlan updatedPlan = floorPlanRepository.save(floorPlan);
            floorPlanRepository.flush();
            
            logger.info("Updated and persisted floor plan: {} with ID: {}", updatedPlan.getName(), updatedPlan.getId());
            return updatedPlan;
        } catch (DataAccessException e) {
            logger.error("Database error updating floor plan with ID: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на планот");
        }
    }

    public void deleteFloorPlan(Long id) {
        try {
            FloorPlan floorPlan = floorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

            floorPlan.setActive(false);
            floorPlanRepository.save(floorPlan);
            floorPlanRepository.flush();
            
            logger.info("Deleted floor plan with ID: {}", id);
        } catch (DataAccessException e) {
            logger.error("Database error deleting floor plan with ID: {}", id, e);
            throw new RuntimeException("Грешка при бришење на планот");
        }
    }

    public FloorPlan setActiveFloorPlan(Long id) {
        try {
            // First, deactivate all floor plans
            List<FloorPlan> allPlans = floorPlanRepository.findAll();
            allPlans.forEach(plan -> plan.setActive(false));
            floorPlanRepository.saveAll(allPlans);

            // Then activate the selected plan
            FloorPlan selectedPlan = floorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

            selectedPlan.setActive(true);
            FloorPlan activatedPlan = floorPlanRepository.save(selectedPlan);
            floorPlanRepository.flush();
            
            logger.info("Activated and persisted floor plan: {} with ID: {}", activatedPlan.getName(), activatedPlan.getId());
            return activatedPlan;
        } catch (DataAccessException e) {
            logger.error("Database error activating floor plan with ID: {}", id, e);
            throw new RuntimeException("Грешка при активирање на планот");
        }
    }

    public FloorPlan createDefaultFloorPlan() {
        try {
            logger.info("Creating default floor plan...");
            
            // Check if any floor plan already exists
            List<FloorPlan> existingPlans = getAllActiveFloorPlans();
            if (!existingPlans.isEmpty()) {
                logger.info("Floor plan already exists, returning existing one");
                FloorPlan existingPlan = existingPlans.get(0);
                return setActiveFloorPlan(existingPlan.getId());
            }
            
            // Create the default floor plan
            FloorPlan defaultPlan = new FloorPlan("Основен план", "Основен план на ресторанот со стандардни области");
            defaultPlan = floorPlanRepository.save(defaultPlan);
            floorPlanRepository.flush();
            logger.info("Created default floor plan with ID: {}", defaultPlan.getId());

            // Create default areas with error handling
            try {
                Area diningArea = createDefaultArea(defaultPlan, "Главна трпезарија", Area.AreaType.DINING, "#3B82F6", 50, 50, 400, 300);
                Area barArea = createDefaultArea(defaultPlan, "Бар", Area.AreaType.BAR, "#8B5CF6", 500, 50, 200, 150);
                Area terraceArea = createDefaultArea(defaultPlan, "Тераса", Area.AreaType.TERRACE, "#10B981", 50, 400, 300, 200);
                
                areaRepository.flush();
                logger.info("Created and persisted default areas for floor plan");

                // Try to create default tables
                try {
                    createDefaultTablesForArea(diningArea, 1, 8);  // Dining area: Tables 1-8
                    createDefaultTablesForArea(barArea, 9, 4);    // Bar area: Tables 9-12
                    createDefaultTablesForArea(terraceArea, 13, 4); // Terrace area: Tables 13-16
                    
                    tableRepository.flush();
                    logger.info("Created and persisted default tables for floor plan");
                } catch (Exception e) {
                    logger.warn("Error creating default tables, but floor plan and areas were created: {}", e.getMessage());
                }
                
            } catch (Exception e) {
                logger.warn("Error creating default areas, but floor plan was created: {}", e.getMessage());
            }

            return defaultPlan;
            
        } catch (DataAccessException e) {
            logger.error("Database error creating default floor plan", e);
            throw new RuntimeException("Грешка при креирање на основниот план");
        } catch (Exception e) {
            logger.error("Unexpected error creating default floor plan", e);
            throw new RuntimeException("Неочекувана грешка при креирање на основниот план: " + e.getMessage());
        }
    }
    
    private Area createDefaultArea(FloorPlan floorPlan, String name, Area.AreaType type, String color, 
                                   int x, int y, int width, int height) {
        try {
            Area area = new Area(name, type, floorPlan);
            area.setColor(color);
            area.setPositionX(x);
            area.setPositionY(y);
            area.setWidth(width);
            area.setHeight(height);
            Area savedArea = areaRepository.save(area);
            logger.info("Created default area: {} with ID: {}", name, savedArea.getId());
            return savedArea;
        } catch (Exception e) {
            logger.error("Error creating default area: {}", name, e);
            throw e;
        }
    }

    private void createDefaultTablesForArea(Area area, int startTableNumber, int tableCount) {
        try {
            int tablesPerRow = (int) Math.ceil(Math.sqrt(tableCount));
            int tableWidth = 60;
            int tableHeight = 60;
            int spacing = 20;

            for (int i = 0; i < tableCount; i++) {
                int tableNumber = startTableNumber + i;
                
                // Check if table already exists to avoid unique constraint violation
                if (!tableRepository.existsByTableNumberAndActiveTrue(tableNumber)) {
                    int row = i / tablesPerRow;
                    int col = i % tablesPerRow;
                    
                    RestaurantTable table = new RestaurantTable(tableNumber, 4, area);
                    table.setPositionX(col * (tableWidth + spacing) + 20);
                    table.setPositionY(row * (tableHeight + spacing) + 20);
                    table.setWidth(tableWidth);
                    table.setHeight(tableHeight);
                    table.setShape(RestaurantTable.TableShape.RECTANGLE);

                    RestaurantTable savedTable = tableRepository.save(table);
                    logger.info("Created default table: {} with ID: {}", tableNumber, savedTable.getId());
                } else {
                    logger.info("Table {} already exists, skipping", tableNumber);
                }
            }
        } catch (Exception e) {
            logger.error("Error creating default tables for area: {}", area.getName(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getAllTablesWithCurrentOrder() {
        try {
            return tableRepository.findAllActiveTablesOrderByNumber();
        } catch (DataAccessException e) {
            logger.error("Database error getting all tables", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<RestaurantTable> getTableByNumber(Integer tableNumber) {
        try {
            if (tableNumber == null) {
                return Optional.empty();
            }
            return tableRepository.findByTableNumberWithArea(tableNumber);
        } catch (DataAccessException e) {
            logger.error("Database error getting table by number: {}", tableNumber, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }
}