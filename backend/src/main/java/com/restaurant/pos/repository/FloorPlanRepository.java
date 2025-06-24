package com.restaurant.pos.repository;

import com.restaurant.pos.entity.FloorPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FloorPlanRepository extends JpaRepository<FloorPlan, Long> {

    // Get all active floor plans ordered by name
    List<FloorPlan> findByActiveTrueOrderByNameAsc();

    // Get any active floor plan (should be only one)
    Optional<FloorPlan> findByActiveTrue();
    
    // Get floor plan with areas (without tables to avoid MultipleBagFetchException)
    @Query("SELECT fp FROM FloorPlan fp LEFT JOIN FETCH fp.areas WHERE fp.active = true")
    Optional<FloorPlan> findActiveFloorPlanWithAreas();

    // Get floor plan by ID with areas
    @Query("SELECT fp FROM FloorPlan fp LEFT JOIN FETCH fp.areas WHERE fp.id = :id")
    Optional<FloorPlan> findByIdWithAreas(Long id);

    // Check if any active floor plan exists
    boolean existsByActiveTrue();
    
    // Count active floor plans
    long countByActiveTrue();
}