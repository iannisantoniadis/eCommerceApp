package com.example.ecomerce.customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Endpoint to create a new customer, usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<String> createCustomer(
            @RequestBody @Valid CustomerRequest request
    ) {
        return ResponseEntity.ok(service.createCustomer(request));
    }

    @PutMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Endpoint to update an existing customer's data, usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Void> updateCustomer(
            @RequestBody @Valid CustomerRequest request
    ) {
        service.updateCustomer(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Endpoint to find all customers, usable by VISITOR")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Page<CustomerResponse>> findAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(service.findAllCustomers(page, size));
    }

    @GetMapping("/exists/{customer-id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Endpoint to check the existence of a particular customer (for the orchestrator), usable by VISITOR")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Boolean> existsById(
            @PathVariable("customer-id") String customerId
    ) {
        var customer = service.findById(customerId);
        return ResponseEntity.ok(customer.isPresent());
    }

    @GetMapping("/{customer-id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VISITOR')")
    @Operation(description = "Endpoint to find a particular visitor by id, usable by VISITOR")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<CustomerResponse> findById(
            @PathVariable("customer-id") String customerId
    ) {
        var customer = service.findByIdResponse(customerId);
        return ResponseEntity.ok(customer);
    }

    @DeleteMapping("/{customer-id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Endpoint to delete an existing customer, usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Void> deleteById(
            @PathVariable("customer-id") String customerId
    ) {
        service.deleteById(customerId);
        return ResponseEntity.accepted().build();
    }
}
