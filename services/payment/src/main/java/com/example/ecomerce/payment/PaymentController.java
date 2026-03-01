package com.example.ecomerce.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Endpoint to save payment and send the notification. Usable by CUSTOMER")
    @SecurityRequirement(name = "Keycloak-JWT")
    public ResponseEntity<Long> createPayment(
            @RequestBody @Valid PaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPayment(request));
    }
}
