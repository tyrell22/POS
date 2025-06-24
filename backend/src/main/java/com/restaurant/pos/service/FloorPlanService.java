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
            
            // Get the first active floor plan
            List<FloorPlan> activeFloorPlans = floorPlanRepository.findByActiveTrueOrderByNameAsc();
            
            if (activeFloorPlans.isEmpty()) {
                logger.info("No active floor plan found");
                return Optional.empty();
            }
            
            FloorPlan floorPlan = activeFloorPlans.get(0);
            logger.info("Found active floor plan: {}", floorPlan.getName());
            
            // Manually load areas for this floor plan
            try {
                List<Area> areas = areaRepository.findByFloorPlanIdAndActiveTrueOrderByNameAsc(floorPlan.getId());
                logger.info("Found {} areas for floor plan", areas.size());
                
                // Load tables for each area
                for (Area area : areas) {
                    try {
                        List<RestaurantTable> tables = tableRepository.findByAreaIdAndActiveTrueOrderByTableNumberAsc(area.getId());
                        area.setTables(tables);
                        logger.info("Area '{}' has {} tables", area.getName(), tables.size());
                    } catch (Exception e) {
                        logger.warn("Error loading tables for area {}: {}", area.getName(), e.getMessage());
                        area.setTables(List.of()); // Set empty list on error
                    }
                }
                
                floorPlan.setAreas(areas);
                
            } catch (Exception e) {
                logger.warn("Error loading areas for floor plan: {}", e.getMessage());
                floorPlan.setAreas(List.of()); // Set empty list on error
            }
            
            return Optional.of(floorPlan);
            
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
            
            Optional<FloorPlan> floorPlanOpt = floorPlanRepository.findById(id);
            
            if (floorPlanOpt.isPresent()) {
                FloorPlan floorPlan = floorPlanOpt.get();
                
                // Load areas manually
                try {
                    List<Area> areas = areaRepository.findByFloorPlanIdAndActiveTrueOrderByNameAsc(id);
                    
                    // Load tables for each area
                    for (Area area : areas) {
                        try {
                            List<RestaurantTable> tables = tableRepository.findByAreaIdAndActiveTrueOrderByTableNumberAsc(area.getId());
                            area.setTables(tables);
                        } catch (Exception e) {
                            logger.warn("Error loading tables for area {}: {}", area.getName(), e.getMessage());
                            area.setTables(List.of());
                        }
                    }
                    
                    floorPlan.setAreas(areas);
                    
                } catch (Exception e) {
                    logger.warn("Error loading areas for floor plan {}: {}", id, e.getMessage());
                    floorPlan.setAreas(List.of());
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
            
            // Initialize empty areas list
            savedPlan.setAreas(List.of());
            
            logger.info("Created floor plan: {} with ID: {}", savedPlan.getName(), savedPlan.getId());
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
            
            logger.info("Updated floor plan: {} with ID: {}", updatedPlan.getName(), updatedPlan.getId());
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
            
            logger.info("Activated floor plan: {} with ID: {}", activatedPlan.getName(), activatedPlan.getId());
            return activatedPlan;
        } catch (DataAccessException e) {
            logger.error("Database error activating floor plan with ID: {}", id, e);
            throw new RuntimeException("Грешка при активирање на планот");
        }
    }

    public FloorPlan createDefaultFloorPlan() {
        try {
            logger.info("Creating simplified default floor plan...");
            
            // Check if any floor plan already exists
            List<FloorPlan> existingPlans = getAllActiveFloorPlans();
            if (!existingPlans.isEmpty()) {
                logger.info("Floor plan already exists, returning existing one");
                FloorPlan existingPlan = existingPlans.get(0);
                return setActiveFloorPlan(existingPlan.getId());
            }
            
            // Create the default floor plan
            FloorPlan defaultPlan = new FloorPlan("Основен план", "Поедноставен план на ресторанот");
            defaultPlan = floorPlanRepository.save(defaultPlan);
            logger.info("Created default floor plan with ID: {}", defaultPlan.getId());

            // Create default areas
            try {
                Area restaurantArea = createDefaultArea(defaultPlan, "Ресторан", Area.AreaType.DINING, "#3B82F6");
                Area terraceArea = createDefaultArea(defaultPlan, "Тераса", Area.AreaType.TERRACE, "#10B981");
                
                // Create default tables
                try {
                    createSimplifiedTablesForArea(restaurantArea, 1, 8);  // Restaurant: Tables 1-8
                    createSimplifiedTablesForArea(terraceArea, 9, 4);    // Terrace: Tables 9-12
                    
                    logger.info("Created simplified table layout");
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
    
    private Area createDefaultArea(FloorPlan floorPlan, String name, Area.AreaType type, String color) {
        try {
            Area area = new Area(name, type, floorPlan);
            area.setColor(color);
            area.setPositionX(0);
            area.setPositionY(0);
            area.setWidth(400);
            area.setHeight(300);
            Area savedArea = areaRepository.save(area);
            logger.info("Created default area: {} with ID: {}", name, savedArea.getId());
            return savedArea;
        } catch (Exception e) {
            logger.error("Error creating default area: {}", name, e);
            throw e;
        }
    }

    private void createSimplifiedTablesForArea(Area area, int startTableNumber, int tableCount) {
        try {
            for (int i = 0; i < tableCount; i++) {
                int tableNumber = startTableNumber + i;
                
                // Check if table already exists to avoid unique constraint violation
                if (!tableRepository.existsByTableNumberAndActiveTrue(tableNumber)) {
                    RestaurantTable table = new RestaurantTable(tableNumber, 4, area);
                    
                    // Simple positioning
                    int gridRow = i / 2;  // 2 columns
                    int gridCol = i % 2;
                    
                    table.setPositionX(gridCol * 150);
                    table.setPositionY(gridRow * 100);
                    table.setWidth(120);
                    table.setHeight(80);
                    table.setShape(RestaurantTable.TableShape.RECTANGLE);

                    RestaurantTable savedTable = tableRepository.save(table);
                    logger.info("Created table: {} with ID: {}", tableNumber, savedTable.getId());
                } else {
                    logger.info("Table {} already exists, skipping", tableNumber);
                }
            }
        } catch (Exception e) {
            logger.error("Error creating tables for area: {}", area.getName(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getAllTablesWithCurrentOrder() {
        try {
            return tableRepository.findByActiveTrueOrderByTableNumberAsc();
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
            return tableRepository.findByTableNumberAndActiveTrue(tableNumber);
        } catch (DataAccessException e) {
            logger.error("Database error getting table by number: {}", tableNumber, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }
}