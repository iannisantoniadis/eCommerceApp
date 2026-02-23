package com.example.ecomerce.orderLine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
//    @Query("select ol from OrderLine ol where ol.order.id = :orderId")
    List<OrderLine> findAllByOrderId(Long orderId);
}
