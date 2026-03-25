package com.example.ecomerce.orderLine;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Service
@RequiredArgsConstructor
@RequestMapping("/api/v1/order-line")
public class OrderLineController {

    private  final OrderLineService service;

    @GetMapping("/order/{order-id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderLineResponse>> findAllByOrderId(
            @PathVariable("order-id") Long orderId
    ) {
        return ResponseEntity.ok(service.findAllByOrderIdToResponse(orderId));
    }
}
