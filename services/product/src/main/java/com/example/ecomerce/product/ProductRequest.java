package com.example.ecomerce.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        @NotNull(message="Product name is mandatory!")
        String name,
        @NotNull(message="Product description is mandatory!")
        String description,
        @Positive(message = "Available quantity must be greater than 0!")
        Double availableQuantity,
        @Positive(message = "Price must be greater than 0!")
        BigDecimal price,

        @NotNull(message = "The product category must be set!")
        Long categoryId) {
}
