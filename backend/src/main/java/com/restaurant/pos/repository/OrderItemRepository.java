package com.restaurant.pos.repository;

import com.restaurant.pos.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}