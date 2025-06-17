package com.restaurant.pos.repository;

import com.restaurant.pos.entity.FloorPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FloorPlanRepository extends JpaRepository<FloorPlan, Long> {

    List<FloorPlan> findByActiveTrueOrderByNameAsc();

    Optional<FloorPlan> findByActiveTrue();
    
    // Fixed: Remove the problematic JOIN FETCH that causes MultipleBagFetchException
    @Query("SELECT fp FROM FloorPlan fp LEFT JOIN FETCH fp.areas WHERE fp.active = true")
    Optional<FloorPlan> findActiveFloorPlanWithAreasAndTables();

    @Query("SELECT fp FROM FloorPlan fp LEFT JOIN FETCH fp.areas WHERE fp.id = :id")
    Optional<FloorPlan> findByIdWithAreas(Long id);

    // Fixed: Remove the problematic JOIN FETCH
    @Query("SELECT fp FROM FloorPlan fp LEFT JOIN FETCH fp.areas WHERE fp.id = :id")
    Optional<FloorPlan> findByIdWithAreasAndTables(Long id);
}