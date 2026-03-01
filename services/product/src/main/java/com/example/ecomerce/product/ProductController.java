package com.example.ecomerce.product;

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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Used to create a new product and add it to the inventory, usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Long> createProduct(
            @RequestBody @Valid ProductRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createProduct(request));
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Accessed by the orchestrator to purchase a product, usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<List<ProductPurchaseResponse>> purchaseProducts(
            @RequestBody List<ProductPurchaseRequest> requestList
    ) {
        return ResponseEntity.ok(service.purchaseProducts(requestList));
    }

    @GetMapping("/{product-id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Used to find a particular item by id, usable by VISITOR")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<ProductResponse> findById(
            @PathVariable("product-id") Long productId
    ) {
        return ResponseEntity.ok(service.findById(productId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Used to check the whole inventory, usable by VISITOR")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
