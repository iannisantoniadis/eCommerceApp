package com.example.ecomerce.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Orchestrator endpoint that creates the order, subtracts the products, commences the payment," +
            " sends the notification." +
            " Usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Long> createOrder(
            @RequestBody @Valid OrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Endpoint to find all orders, commences the payment. Usable by VISITOR")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<List<OrderResponse>> findAll(){
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{order-id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Endpoint to find one order in particular. Usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable("order-id") Long orderId
    ) {
        return ResponseEntity.ok(service.findById(orderId));
    }
}
