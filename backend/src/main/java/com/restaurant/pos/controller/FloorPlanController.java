package com.restaurant.pos.controller;

import com.restaurant.pos.entity.FloorPlan;
import com.restaurant.pos.entity.Area;
import com.restaurant.pos.entity.RestaurantTable;
import com.restaurant.pos.dto.FloorPlanRequest;
import com.restaurant.pos.dto.AreaRequest;
import com.restaurant.pos.service.FloorPlanService;
import com.restaurant.pos.service.AreaService;
import com.restaurant.pos.service.DatabaseHealthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/floor-plans")
@CrossOrigin(origins = "*")
public class FloorPlanController {

    private static final Logger logger = LoggerFactory.getLogger(FloorPlanController.class);

    @Autowired
    private FloorPlanService floorPlanService;

    @Autowired
    private AreaService areaService;
    
    @Autowired
    private DatabaseHealthService databaseHealthService;

    // Floor Plan Management
    @GetMapping
    public ResponseEntity<?> getAllFloorPlans() {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            List<FloorPlan> floorPlans = floorPlanService.getAllActiveFloorPlans();
            logger.info("Retrieved {} floor plans", floorPlans.size());
            return ResponseEntity.ok(floorPlans);
        } catch (Exception e) {
            logger.error("Error getting all floor plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на плановите"));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveFloorPlan() {
        try {
            logger.info("Getting active floor plan...");
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Optional<FloorPlan> activeFloorPlan = floorPlanService.getActiveFloorPlan();
            
            if (activeFloorPlan.isPresent()) {
                logger.info("Found active floor plan: {}", activeFloorPlan.get().getName());
                return ResponseEntity.ok(activeFloorPlan.get());
            } else {
                logger.info("No active floor plan found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Нема активен план"));
            }
        } catch (Exception e) {
            logger.error("Error getting active floor plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на активниот план"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFloorPlanById(@PathVariable Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Optional<FloorPlan> floorPlan = floorPlanService.getFloorPlanById(id);
            
            if (floorPlan.isPresent()) {
                return ResponseEntity.ok(floorPlan.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Планот не е пронајден"));
            }
        } catch (Exception e) {
            logger.error("Error getting floor plan by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на планот"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createFloorPlan(@Valid @RequestBody FloorPlanRequest request) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            FloorPlan floorPlan = floorPlanService.createFloorPlan(request.getName(), request.getDescription());
            logger.info("Created floor plan: {} with ID: {}", floorPlan.getName(), floorPlan.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(floorPlan);
        } catch (Exception e) {
            logger.error("Error creating floor plan", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFloorPlan(@PathVariable Long id, @Valid @RequestBody FloorPlanRequest request) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            FloorPlan floorPlan = floorPlanService.updateFloorPlan(id, request.getName(), request.getDescription());
            return ResponseEntity.ok(floorPlan);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Планот не е пронајден"));
        } catch (Exception e) {
            logger.error("Error updating floor plan: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFloorPlan(@PathVariable Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            floorPlanService.deleteFloorPlan(id);
            return ResponseEntity.ok(Map.of("message", "Планот е избришан успешно"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateFloorPlan(@PathVariable Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            FloorPlan floorPlan = floorPlanService.setActiveFloorPlan(id);
            return ResponseEntity.ok(Map.of(
                "message", "Планот е активиран успешно", 
                "floorPlan", floorPlan
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Планот не е пронајден"));
        }
    }

    @PostMapping("/default")
    public ResponseEntity<?> createDefaultFloorPlan() {
        try {
            logger.info("Creating default floor plan...");
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                logger.error("Database is not healthy");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Check if a floor plan already exists
            List<FloorPlan> existingPlans = floorPlanService.getAllActiveFloorPlans();
            if (!existingPlans.isEmpty()) {
                logger.info("Floor plan already exists, returning first one");
                FloorPlan existingPlan = existingPlans.get(0);
                // Make sure it's active
                floorPlanService.setActiveFloorPlan(existingPlan.getId());
                return ResponseEntity.ok(existingPlan);
            }
            
            FloorPlan floorPlan = floorPlanService.createDefaultFloorPlan();
            logger.info("Created default floor plan: {} with ID: {}", floorPlan.getName(), floorPlan.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(floorPlan);
            
        } catch (Exception e) {
            logger.error("Error creating default floor plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при креирање на основниот план: " + e.getMessage()));
        }
    }

    // Area Management
    @GetMapping("/{floorPlanId}/areas")
    public ResponseEntity<?> getAreasForFloorPlan(@PathVariable Long floorPlanId) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            List<Area> areas = areaService.getAreasByFloorPlan(floorPlanId);
            return ResponseEntity.ok(areas);
        } catch (Exception e) {
            logger.error("Error getting areas for floor plan: {}", floorPlanId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на областите"));
        }
    }

    @PostMapping("/areas")
    public ResponseEntity<?> createArea(@Valid @RequestBody AreaRequest request) {
        try {
            logger.info("Creating area with request: {}", request);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate required fields
            if (request.getFloorPlanId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID на план е задолжително"));
            }
            
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Име на област е задолжително"));
            }
            
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Тип на област е задолжителен"));
            }
            
            Area.AreaType type = Area.AreaType.valueOf(request.getType());
            Area area = areaService.createArea(
                request.getFloorPlanId(),
                request.getName(),
                request.getDescription(),
                type,
                request.getColor()
            );
            
            logger.info("Created area: {} with ID: {}", area.getName(), area.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(area);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid area type: {}", request.getType(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Неважечки тип на област"));
        } catch (Exception e) {
            logger.error("Error creating area", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/areas/{id}")
    public ResponseEntity<?> updateArea(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Area.AreaType type = Area.AreaType.valueOf(request.getType());
            Area area = areaService.updateArea(id, request.getName(), request.getDescription(), type, request.getColor());
            return ResponseEntity.ok(area);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неважечки тип на област"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<?> deleteArea(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
        try {
            logger.info("Deleting area with ID: {} (force: {})", id, force);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            if (id == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID на област е задолжително"));
            }
            
            if (force) {
                areaService.deleteAreaForce(id);
            } else {
                areaService.deleteArea(id);
            }
            return ResponseEntity.ok(Map.of("message", "Областа е избришана успешно"));
        } catch (RuntimeException e) {
            logger.warn("Error deleting area: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting area: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при бришење на областа"));
        }
    }

    @GetMapping("/areas/{id}")
    public ResponseEntity<?> getAreaById(@PathVariable Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Optional<Area> area = areaService.getAreaById(id);
            if (area.isPresent()) {
                return ResponseEntity.ok(area.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Областа не е пронајдена"));
            }
        } catch (Exception e) {
            logger.error("Error getting area by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на областа"));
        }
    }

    // Simplified Table Management for Grid Layout
    @PostMapping("/areas/{areaId}/tables")
    public ResponseEntity<?> addTableToArea(@PathVariable Long areaId, @RequestBody Map<String, Object> tableData) {
        try {
            logger.info("Adding table to area ID: {} with data: {}", areaId, tableData);
            
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            // Validate area ID
            if (areaId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID на област е задолжително"));
            }
            
            // Validate required table data
            Object tableNumberObj = tableData.get("tableNumber");
            Object capacityObj = tableData.get("capacity");
            
            if (tableNumberObj == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Број на маса е задолжителен"));
            }
            
            if (capacityObj == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Капацитет е задолжителен"));
            }

            Integer tableNumber;
            Integer capacity;
            
            try {
                tableNumber = Integer.valueOf(tableNumberObj.toString());
                capacity = Integer.valueOf(capacityObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неважечки бројни вредности"));
            }
            
            // Set default values for simplified grid layout
            tableData.put("shape", "RECTANGLE");
            tableData.put("status", "AVAILABLE");
            tableData.put("width", 120);
            tableData.put("height", 80);
            
            Area area = areaService.addTableToArea(areaId, tableData);
            return ResponseEntity.status(HttpStatus.CREATED).body(area);
            
        } catch (Exception e) {
            logger.error("Error adding table to area: {}", areaId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Table Management
    @GetMapping("/tables")
    public ResponseEntity<?> getAllTablesWithOrders() {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            List<RestaurantTable> tables = floorPlanService.getAllTablesWithCurrentOrder();
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            logger.error("Error getting all tables with orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на масите"));
        }
    }

    @GetMapping("/tables/{tableNumber}")
    public ResponseEntity<?> getTableByNumber(@PathVariable Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }
            
            Optional<RestaurantTable> table = floorPlanService.getTableByNumber(tableNumber);
            if (table.isPresent()) {
                return ResponseEntity.ok(table.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Масата не е пронајдена"));
            }
        } catch (Exception e) {
            logger.error("Error getting table by number: {}", tableNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на масата"));
        }
    }

    // Utility endpoints
    @GetMapping("/area-types")
    public ResponseEntity<Area.AreaType[]> getAreaTypes() {
        return ResponseEntity.ok(Area.AreaType.values());
    }

    @GetMapping("/table-shapes")
    public ResponseEntity<RestaurantTable.TableShape[]> getTableShapes() {
        return ResponseEntity.ok(RestaurantTable.TableShape.values());
    }
}