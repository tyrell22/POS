package com.restaurant.pos.repository;

import com.restaurant.pos.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByAvailableTrueOrderByNameAsc();
    List<MenuItem> findByCategoryAndAvailableTrueOrderByNameAsc(MenuItem.Category category);
    List<MenuItem> findByNameContainingIgnoreCaseAndAvailableTrueOrderByNameAsc(String name);
    List<MenuItem> findAllByOrderByNameAsc();
}