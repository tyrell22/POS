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
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FloorPlanService {

    @Autowired
    private FloorPlanRepository floorPlanRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    public List<FloorPlan> getAllActiveFloorPlans() {
        return floorPlanRepository.findByActiveTrueOrderByNameAsc();
    }

    public Optional<FloorPlan> getActiveFloorPlan() {
        Optional<FloorPlan> floorPlanOpt = floorPlanRepository.findActiveFloorPlanWithAreasAndTables();
        
        // Manually load tables for each area to avoid MultipleBag issue
        if (floorPlanOpt.isPresent()) {
            FloorPlan floorPlan = floorPlanOpt.get();
            for (Area area : floorPlan.getAreas()) {
                area.getTables().size(); // Force load tables
            }
        }
        
        return floorPlanOpt;
    }

    public Optional<FloorPlan> getFloorPlanById(Long id) {
        Optional<FloorPlan> floorPlanOpt = floorPlanRepository.findByIdWithAreasAndTables(id);
        
        // Manually load tables for each area
        if (floorPlanOpt.isPresent()) {
            FloorPlan floorPlan = floorPlanOpt.get();
            for (Area area : floorPlan.getAreas()) {
                area.getTables().size(); // Force load tables
            }
        }
        
        return floorPlanOpt;
    }

    public FloorPlan createFloorPlan(String name, String description) {
        FloorPlan floorPlan = new FloorPlan(name, description);
        return floorPlanRepository.save(floorPlan);
    }

    public FloorPlan updateFloorPlan(Long id, String name, String description) {
        FloorPlan floorPlan = floorPlanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

        floorPlan.setName(name);
        floorPlan.setDescription(description);

        return floorPlanRepository.save(floorPlan);
    }

    public void deleteFloorPlan(Long id) {
        FloorPlan floorPlan = floorPlanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

        floorPlan.setActive(false);
        floorPlanRepository.save(floorPlan);
    }

    public FloorPlan setActiveFloorPlan(Long id) {
        // Deactivate all floor plans
        List<FloorPlan> allPlans = floorPlanRepository.findAll();
        allPlans.forEach(plan -> plan.setActive(false));
        floorPlanRepository.saveAll(allPlans);

        // Activate the selected plan
        FloorPlan selectedPlan = floorPlanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Планот на ресторанот не е пронајден"));

        selectedPlan.setActive(true);
        return floorPlanRepository.save(selectedPlan);
    }

    public FloorPlan createDefaultFloorPlan() {
        // Check if floor plan already exists to avoid duplicates
        Optional<FloorPlan> existing = floorPlanRepository.findByActiveTrue();
        if (existing.isPresent()) {
            return existing.get();
        }
        
        FloorPlan defaultPlan = new FloorPlan("Основен план", "Основен план на ресторанот со стандардни области");
        defaultPlan = floorPlanRepository.save(defaultPlan);

        // Create default areas
        Area diningArea = new Area("Главна трпезарија", Area.AreaType.DINING, defaultPlan);
        diningArea.setColor("#3B82F6");
        diningArea.setPositionX(50);
        diningArea.setPositionY(50);
        diningArea.setWidth(400);
        diningArea.setHeight(300);
        areaRepository.save(diningArea);
        
        Area barArea = new Area("Бар", Area.AreaType.BAR, defaultPlan);
        barArea.setColor("#8B5CF6");
        barArea.setPositionX(500);
        barArea.setPositionY(50);
        barArea.setWidth(200);
        barArea.setHeight(150);
        areaRepository.save(barArea);
        
        Area terraceArea = new Area("Тераса", Area.AreaType.TERRACE, defaultPlan);
        terraceArea.setColor("#10B981");
        terraceArea.setPositionX(50);
        terraceArea.setPositionY(400);
        terraceArea.setWidth(300);
        terraceArea.setHeight(200);
        areaRepository.save(terraceArea);

        // Create default tables with duplicate check
        createDefaultTablesForArea(diningArea, 1, 8); // Tables 1-8
        createDefaultTablesForArea(barArea, 9, 4);    // Tables 9-12
        createDefaultTablesForArea(terraceArea, 13, 4); // Tables 13-16

        return defaultPlan;
    }

    private void createDefaultTablesForArea(Area area, int startTableNumber, int tableCount) {
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

                tableRepository.save(table);
            }
        }
    }

    public List<RestaurantTable> getAllTablesWithCurrentOrder() {
        // Fixed: Use the correct method name from your repository
        return tableRepository.findAllActiveTablesOrderByNumber();
    }

    public Optional<RestaurantTable> getTableByNumber(Integer tableNumber) {
        return tableRepository.findByTableNumberWithArea(tableNumber);
    }
}