package com.restaurant.pos.controller;

import com.restaurant.pos.entity.FloorPlan;
import com.restaurant.pos.entity.Area;
import com.restaurant.pos.entity.RestaurantTable;
import com.restaurant.pos.dto.FloorPlanRequest;
import com.restaurant.pos.dto.AreaRequest;
import com.restaurant.pos.service.FloorPlanService;
import com.restaurant.pos.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/floor-plans")
@CrossOrigin(origins = "*")
public class FloorPlanController {

    @Autowired
    private FloorPlanService floorPlanService;

    @Autowired
    private AreaService areaService;

    // Floor Plan Management
    @GetMapping
    public ResponseEntity<List<FloorPlan>> getAllFloorPlans() {
        List < FloorPlan > floorPlans = floorPlanService.getAllActiveFloorPlans();
        return ResponseEntity.ok(floorPlans);
    }

        @GetMapping("/active")
        public ResponseEntity<FloorPlan> getActiveFloorPlan() {
        return floorPlanService.getActiveFloorPlan()
            .map(plan -> ResponseEntity.ok(plan))
            .orElse(ResponseEntity.notFound().build());
    }

            @GetMapping("/{id}")
            public ResponseEntity<FloorPlan> getFloorPlanById(@PathVariable Long id) {
        return floorPlanService.getFloorPlanById(id)
            .map(plan -> ResponseEntity.ok(plan))
                .orElse(ResponseEntity.notFound().build());
    }

                @PostMapping
                public ResponseEntity<?> createFloorPlan(@Valid @RequestBody FloorPlanRequest request) {
        try {
                    FloorPlan floorPlan = floorPlanService.createFloorPlan(request.getName(), request.getDescription());
                return ResponseEntity.status(HttpStatus.CREATED).body(floorPlan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                @PutMapping("/{id}")
                public ResponseEntity<?> updateFloorPlan(@PathVariable Long id, @Valid @RequestBody FloorPlanRequest request) {
        try {
                    FloorPlan floorPlan = floorPlanService.updateFloorPlan(id, request.getName(), request.getDescription());
                return ResponseEntity.ok(floorPlan);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                @DeleteMapping("/{id}")
                public ResponseEntity<?> deleteFloorPlan(@PathVariable Long id) {
        try {
                    floorPlanService.deleteFloorPlan(id);
                return ResponseEntity.ok(Map.of("message", "Планот е избришан успешно"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                @PostMapping("/{id}/activate")
                public ResponseEntity<?> activateFloorPlan(@PathVariable Long id) {
        try {
                    FloorPlan floorPlan = floorPlanService.setActiveFloorPlan(id);
                return ResponseEntity.ok(Map.of("message", "Планот е активиран успешно", "floorPlan", floorPlan));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

                @PostMapping("/default")
                public ResponseEntity<?> createDefaultFloorPlan() {
        try {
                    FloorPlan floorPlan = floorPlanService.createDefaultFloorPlan();
                return ResponseEntity.status(HttpStatus.CREATED).body(floorPlan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                // Area Management
                @GetMapping("/{floorPlanId}/areas")
                public ResponseEntity<List<Area>> getAreasForFloorPlan(@PathVariable Long floorPlanId) {
                    List < Area > areas = areaService.getAreasByFloorPlan(floorPlanId);
                    return ResponseEntity.ok(areas);
    }

                    @PostMapping("/areas")
                    public ResponseEntity<?> createArea(@Valid @RequestBody AreaRequest request) {
        try {
                        Area.AreaType type = Area.AreaType.valueOf(request.getType());
                    Area area = areaService.createArea(
                    request.getFloorPlanId(),
                    request.getName(),
                    request.getDescription(),
                    type,
                    request.getColor()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED).body(area);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неважечки тип на област"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                    @PutMapping("/areas/{id}")
                    public ResponseEntity<?> updateArea(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        try {
                        Area.AreaType type = Area.AreaType.valueOf(request.getType());
                    Area area = areaService.updateArea(id, request.getName(), request.getDescription(), type, request.getColor());
                    return ResponseEntity.ok(area);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неважечки тип на област"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                    @PatchMapping("/areas/{id}/position")
                    public ResponseEntity<?> updateAreaPosition(@PathVariable Long id, @RequestBody Map<String, Integer> position) {
        try {
                        Integer x = position.get("x");
                    Integer y = position.get("y");
                    Integer width = position.get("width");
                    Integer height = position.get("height");

                    Area area = areaService.updateAreaPosition(id, x, y, width, height);
                    return ResponseEntity.ok(area);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                    @DeleteMapping("/areas/{id}")
                    public ResponseEntity<?> deleteArea(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
        try {
            if (force) {
                        areaService.deleteAreaForce(id);
            } else {
                        areaService.deleteArea(id);
            }
                    return ResponseEntity.ok(Map.of("message", "Областа е избришана успешно"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                    @GetMapping("/areas/{id}")
                    public ResponseEntity<Area> getAreaById(@PathVariable Long id) {
        return areaService.getAreaById(id)
            .map(area -> ResponseEntity.ok(area))
                        .orElse(ResponseEntity.notFound().build());
    }

                        @PostMapping("/areas/{areaId}/tables")
                        public ResponseEntity<?> addTableToArea(@PathVariable Long areaId, @RequestBody Map<String, Object> tableData) {
        try {
                            Integer tableNumber = (Integer) tableData.get("tableNumber");
                        Integer capacity = (Integer) tableData.get("capacity");
                        Integer x = (Integer) tableData.get("x");
                        Integer y = (Integer) tableData.get("y");

                        Area area = areaService.addTableToArea(areaId, tableNumber, capacity, x, y);
                        return ResponseEntity.status(HttpStatus.CREATED).body(area);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

                        // Table Management
                        @GetMapping("/tables")
                        public ResponseEntity<List<RestaurantTable>> getAllTablesWithOrders() {
                            List < RestaurantTable > tables = floorPlanService.getAllTablesWithCurrentOrder();
                            return ResponseEntity.ok(tables);
    }

                            @GetMapping("/tables/{tableNumber}")
                            public ResponseEntity<RestaurantTable> getTableByNumber(@PathVariable Integer tableNumber) {
        return floorPlanService.getTableByNumber(tableNumber)
            .map(table -> ResponseEntity.ok(table))
                                .orElse(ResponseEntity.notFound().build());
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