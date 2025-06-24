package com.restaurant.pos.repository;

import com.restaurant.pos.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatusOrderByCreatedAtAsc(Order.Status status);
    Optional<Order> findByTableNumberAndStatus(Integer tableNumber, Order.Status status);
    List<Order> findByStatusInOrderByCreatedAtAsc(List<Order.Status> statuses);

    @Query("SELECT o FROM Order o WHERE o.status IN ('ОТВОРЕНА', 'ИСПРАТЕНА') ORDER BY o.createdAt ASC")
    List<Order> findAllActiveOrders();
}