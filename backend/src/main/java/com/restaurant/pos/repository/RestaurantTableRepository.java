package com.restaurant.pos.repository;

import com.restaurant.pos.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findByActiveTrueOrderByTableNumberAsc();
    List<RestaurantTable> findByAreaIdAndActiveTrueOrderByTableNumberAsc(Long areaId);
    List<RestaurantTable> findByStatusAndActiveTrueOrderByTableNumberAsc(RestaurantTable.TableStatus status);
    Optional<RestaurantTable> findByTableNumberAndActiveTrue(Integer tableNumber);
    
    // FIXED: Remove the problematic query that references currentOrder
    // @Query("SELECT rt FROM RestaurantTable rt LEFT JOIN FETCH rt.currentOrder WHERE rt.active = true ORDER BY rt.tableNumber ASC")
    // List<RestaurantTable> findAllWithCurrentOrderByActiveTrue();
    
    // Replace with a simple query that doesn't reference currentOrder
    @Query("SELECT rt FROM RestaurantTable rt WHERE rt.active = true ORDER BY rt.tableNumber ASC")
    List<RestaurantTable> findAllActiveTablesOrderByNumber();

    @Query("SELECT rt FROM RestaurantTable rt LEFT JOIN FETCH rt.area WHERE rt.tableNumber = :tableNumber AND rt.active = true")
    Optional<RestaurantTable> findByTableNumberWithArea(Integer tableNumber);
    
    boolean existsByTableNumberAndActiveTrue(Integer tableNumber);

    @Query("SELECT COUNT(rt) FROM RestaurantTable rt WHERE rt.area.id = :areaId AND rt.active = true")
    Long countByAreaId(Long areaId);

    @Query("SELECT MAX(rt.tableNumber) FROM RestaurantTable rt WHERE rt.active = true")
    Optional<Integer> findMaxTableNumber();
}