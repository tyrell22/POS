package com.restaurant.pos.repository;

import com.restaurant.pos.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByFloorPlanIdAndActiveTrueOrderByNameAsc(Long floorPlanId);

    List<Area> findByActiveTrueOrderByNameAsc();

    List<Area> findByTypeAndActiveTrueOrderByNameAsc(Area.AreaType type);
    
    @Query("SELECT a FROM Area a LEFT JOIN FETCH a.tables WHERE a.id = :id")
Optional < Area > findByIdWithTables(Long id);

@Query("SELECT a FROM Area a LEFT JOIN FETCH a.tables WHERE a.floorPlan.id = :floorPlanId AND a.active = true")
List < Area > findByFloorPlanIdWithTables(Long floorPlanId);
    
    boolean existsByNameAndFloorPlanId(String name, Long floorPlanId);
}