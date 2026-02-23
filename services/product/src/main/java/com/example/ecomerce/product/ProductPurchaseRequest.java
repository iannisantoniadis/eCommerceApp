package com.example.ecomerce.product;

import jakarta.validation.constraints.NotNull;

public record ProductPurchaseRequest(
        @NotNull(message = "The productId is mandatory!")
        Long productId,
        @NotNull(message = "The quantity is mandatory!")
        Double quantity

) {
}
