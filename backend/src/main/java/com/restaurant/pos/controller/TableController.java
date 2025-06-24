package com.restaurant.pos.controller;

import com.restaurant.pos.entity.RestaurantTable;
import com.restaurant.pos.service.TableService;
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
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {

    private static final Logger logger = LoggerFactory.getLogger(TableController.class);

    @Autowired
    private TableService tableService;

    @Autowired
    private DatabaseHealthService databaseHealthService;

    @GetMapping
    public ResponseEntity<?> getAllTables() {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }

            List < RestaurantTable > tables = tableService.getAllActiveTables();
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            logger.error("Error getting all tables", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на масите"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTableById(@PathVariable Long id) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }

            Optional < RestaurantTable > table = tableService.getTableById(id);
            if (table.isPresent()) {
                return ResponseEntity.ok(table.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Масата не е пронајдена"));
            }
        } catch (Exception e) {
            logger.error("Error getting table by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Грешка при вчитување на масата"));
        }
    }

    @GetMapping("/number/{tableNumber}")
    public ResponseEntity<?> getTableByNumber(@PathVariable Integer tableNumber) {
        try {
            if (!databaseHealthService.isDatabaseHealthy()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "База на податоци недостапна"));
            }

            Optional < RestaurantTable > table = tableService.getTableByNumber(tableNumber);
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @RequestBody Map<String, Object> tableData) {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }
            
            RestaurantTable updatedTable = tableService.updateTable(id, tableData);
        return ResponseEntity.ok(updatedTable);
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        logger.error("Error updating table: {}", id, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при ажурирање на масата"));
    }
}

@PatchMapping("/{id}/position")
public ResponseEntity <?> updateTablePosition(@PathVariable Long id, @RequestBody Map < String, Integer > position) {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }
            
            Integer x = position.get("x");
            Integer y = position.get("y");
            Integer width = position.get("width");
            Integer height = position.get("height");

            RestaurantTable updatedTable = tableService.updateTablePosition(id, x, y, width, height);
        return ResponseEntity.ok(updatedTable);
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        logger.error("Error updating table position: {}", id, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при ажурирање на позицијата"));
    }
}

@PatchMapping("/{id}/status")
public ResponseEntity <?> updateTableStatus(@PathVariable Long id, @RequestBody Map < String, String > statusData) {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }
            
            String status = statusData.get("status");
        if (status == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Статусот е задолжителен"));
        }

        RestaurantTable.TableStatus tableStatus = RestaurantTable.TableStatus.valueOf(status);
            RestaurantTable updatedTable = tableService.updateTableStatus(id, tableStatus);
        return ResponseEntity.ok(updatedTable);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Неважечки статус"));
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        logger.error("Error updating table status: {}", id, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при ажурирање на статусот"));
    }
}

@DeleteMapping("/{id}")
public ResponseEntity <?> deleteTable(@PathVariable Long id) {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }

        tableService.deleteTable(id);
        return ResponseEntity.ok(Map.of("message", "Масата е избришана успешно"));
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        logger.error("Error deleting table: {}", id, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при бришење на масата"));
    }
}

@GetMapping("/area/{areaId}")
public ResponseEntity <?> getTablesByArea(@PathVariable Long areaId) {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }

        List < RestaurantTable > tables = tableService.getTablesByArea(areaId);
        return ResponseEntity.ok(tables);
    } catch (Exception e) {
        logger.error("Error getting tables by area: {}", areaId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при вчитување на масите"));
    }
}

@GetMapping("/status/{status}")
public ResponseEntity <?> getTablesByStatus(@PathVariable String status) {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }

        RestaurantTable.TableStatus tableStatus = RestaurantTable.TableStatus.valueOf(status);
        List < RestaurantTable > tables = tableService.getTablesByStatus(tableStatus);
        return ResponseEntity.ok(tables);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Неважечки статус"));
    } catch (Exception e) {
        logger.error("Error getting tables by status: {}", status, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при вчитување на масите"));
    }
}

@GetMapping("/available")
public ResponseEntity <?> getAvailableTables() {
    try {
        if (!databaseHealthService.isDatabaseHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "База на податоци недостапна"));
        }

        List < RestaurantTable > tables = tableService.getAvailableTables();
        return ResponseEntity.ok(tables);
    } catch (Exception e) {
        logger.error("Error getting available tables", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Грешка при вчитување на достапните маси"));
    }
}
}