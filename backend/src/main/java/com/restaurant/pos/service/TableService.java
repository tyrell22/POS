package com.restaurant.pos.service;

import com.restaurant.pos.entity.RestaurantTable;
import com.restaurant.pos.entity.Area;
import com.restaurant.pos.repository.RestaurantTableRepository;
import com.restaurant.pos.repository.AreaRepository;
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
public class TableService {

    private static final Logger logger = LoggerFactory.getLogger(TableService.class);

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public List<RestaurantTable> getAllActiveTables() {
        try {
            return tableRepository.findAllActiveTablesOrderByNumber();
        } catch (DataAccessException e) {
            logger.error("Database error getting all active tables", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<RestaurantTable> getTableById(Long id) {
        try {
            if (id == null) {
                return Optional.empty();
            }
            return tableRepository.findById(id);
        } catch (DataAccessException e) {
            logger.error("Database error getting table by ID: {}", id, e);
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

    @Transactional(readOnly = true)
    public List<RestaurantTable> getTablesByArea(Long areaId) {
        try {
            return tableRepository.findByAreaIdAndActiveTrueOrderByTableNumberAsc(areaId);
        } catch (DataAccessException e) {
            logger.error("Database error getting tables by area: {}", areaId, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getTablesByStatus(RestaurantTable.TableStatus status) {
        try {
            return tableRepository.findByStatusAndActiveTrueOrderByTableNumberAsc(status);
        } catch (DataAccessException e) {
            logger.error("Database error getting tables by status: {}", status, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getAvailableTables() {
        try {
            return tableRepository.findByStatusAndActiveTrueOrderByTableNumberAsc(RestaurantTable.TableStatus.AVAILABLE);
        } catch (DataAccessException e) {
            logger.error("Database error getting available tables", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    public RestaurantTable createTable(Long areaId, Map<String, Object> tableData) {
    try {
            Area area = areaRepository.findById(areaId)
            .orElseThrow(() -> new RuntimeException("Областа не е пронајдена"));

            // Extract table data
            Integer tableNumber = getIntegerFromMap(tableData, "tableNumber");
            Integer capacity = getIntegerFromMap(tableData, "capacity");
            String shapeStr = (String) tableData.get("shape");
            Integer positionX = getIntegerFromMap(tableData, "positionX");
            Integer positionY = getIntegerFromMap(tableData, "positionY");
            Integer width = getIntegerFromMap(tableData, "width");
            Integer height = getIntegerFromMap(tableData, "height");

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

            // Create table
            RestaurantTable table = new RestaurantTable(tableNumber, capacity, area);

        if (shapeStr != null) {
            try {
                table.setShape(RestaurantTable.TableShape.valueOf(shapeStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid table shape: {}, using default", shapeStr);
            }
        }

        table.setPositionX(positionX != null ? positionX : 0);
        table.setPositionY(positionY != null ? positionY : 0);
        table.setWidth(width != null ? width : 80);
        table.setHeight(height != null ? height : 80);

            RestaurantTable savedTable = tableRepository.save(table);
        logger.info("Created table: {} in area: {}", savedTable.getTableNumber(), area.getName());
        return savedTable;

    } catch (DataAccessException e) {
        logger.error("Database error creating table", e);
        throw new RuntimeException("Грешка при креирање на масата");
    }
}

    public RestaurantTable updateTable(Long tableId, Map < String, Object > tableData) {
    try {
            RestaurantTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

            // Update fields if provided
            Integer tableNumber = getIntegerFromMap(tableData, "tableNumber");
            Integer capacity = getIntegerFromMap(tableData, "capacity");
            String shapeStr = (String) tableData.get("shape");
            String statusStr = (String) tableData.get("status");
            Integer positionX = getIntegerFromMap(tableData, "positionX");
            Integer positionY = getIntegerFromMap(tableData, "positionY");
            Integer width = getIntegerFromMap(tableData, "width");
            Integer height = getIntegerFromMap(tableData, "height");

        if (tableNumber != null) {
            // Check if new table number conflicts with existing tables
            if (!table.getTableNumber().equals(tableNumber) &&
                tableRepository.existsByTableNumberAndActiveTrue(tableNumber)) {
                throw new RuntimeException("Масата со овој број веќе постои");
            }
            table.setTableNumber(tableNumber);
        }

        if (capacity != null) {
            table.setCapacity(capacity);
        }

        if (shapeStr != null) {
            try {
                table.setShape(RestaurantTable.TableShape.valueOf(shapeStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid table shape: {}", shapeStr);
            }
        }

        if (statusStr != null) {
            try {
                table.setStatus(RestaurantTable.TableStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid table status: {}", statusStr);
            }
        }

        if (positionX != null) table.setPositionX(positionX);
        if (positionY != null) table.setPositionY(positionY);
        if (width != null) table.setWidth(width);
        if (height != null) table.setHeight(height);

            RestaurantTable updatedTable = tableRepository.save(table);
        logger.info("Updated table: {}", updatedTable.getTableNumber());
        return updatedTable;

    } catch (DataAccessException e) {
        logger.error("Database error updating table: {}", tableId, e);
        throw new RuntimeException("Грешка при ажурирање на масата");
    }
}

    public RestaurantTable updateTablePosition(Long tableId, Integer x, Integer y, Integer width, Integer height) {
    try {
            RestaurantTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

        if (x != null) table.setPositionX(x);
        if (y != null) table.setPositionY(y);
        if (width != null) table.setWidth(width);
        if (height != null) table.setHeight(height);

            RestaurantTable updatedTable = tableRepository.save(table);
        logger.info("Updated position for table: {}", updatedTable.getTableNumber());
        return updatedTable;

    } catch (DataAccessException e) {
        logger.error("Database error updating table position: {}", tableId, e);
        throw new RuntimeException("Грешка при ажурирање на позицијата");
    }
}

    public RestaurantTable updateTableStatus(Long tableId, RestaurantTable.TableStatus status) {
    try {
            RestaurantTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

        table.setStatus(status);
            RestaurantTable updatedTable = tableRepository.save(table);

        logger.info("Updated status for table {} to: {}", updatedTable.getTableNumber(), status);
        return updatedTable;

    } catch (DataAccessException e) {
        logger.error("Database error updating table status: {}", tableId, e);
        throw new RuntimeException("Грешка при ажурирање на статусот");
    }
}

    public void deleteTable(Long tableId) {
    try {
            RestaurantTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

        // Soft delete - mark as inactive
        table.setActive(false);
        tableRepository.save(table);

        logger.info("Deleted table: {}", table.getTableNumber());

    } catch (DataAccessException e) {
        logger.error("Database error deleting table: {}", tableId, e);
        throw new RuntimeException("Грешка при бришење на масата");
    }
}

    public void deleteTableHard(Long tableId) {
    try {
            RestaurantTable table = tableRepository.findById(tableId)
            .orElseThrow(() -> new RuntimeException("Масата не е пронајдена"));

        // Hard delete - remove from database
        tableRepository.delete(table);

        logger.info("Hard deleted table: {}", table.getTableNumber());

    } catch (DataAccessException e) {
        logger.error("Database error hard deleting table: {}", tableId, e);
        throw new RuntimeException("Грешка при бришење на масата");
    }
}

@Transactional(readOnly = true)
public Long getTableCountByArea(Long areaId) {
    try {
        return tableRepository.countByAreaId(areaId);
    } catch (DataAccessException e) {
        logger.error("Database error counting tables by area: {}", areaId, e);
        throw new RuntimeException("Грешка при броење на масите");
    }
}

@Transactional(readOnly = true)
public Integer getNextTableNumber() {
    try {
        Optional < Integer > maxNumber = tableRepository.findMaxTableNumber();
        return maxNumber.orElse(0) + 1;
    } catch (DataAccessException e) {
        logger.error("Database error getting next table number", e);
        return 1; // Fallback to 1
    }
}

    // Helper method to safely extract integers from map
    private Integer getIntegerFromMap(Map < String, Object > map, String key) {
        Object value = map.get(key);
    if (value == null) return null;

    if (value instanceof Integer) {
        return (Integer) value;
    } else if (value instanceof Number) {
        return ((Number) value).intValue();
    } else if (value instanceof String) {
        try {
            return Integer.parseInt((String) value);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse integer from string: {}", value);
            return null;
        }
    }

    return null;
}
}