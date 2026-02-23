package com.example.ecomerce.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public record Customer(
        String id,
        @NotNull(message = "The firstname is required!")
        @NotBlank(message = "The firstname is required!")
        String firstname,
        @NotNull(message = "The lastname is required!")
        @NotBlank(message = "The lastname is required!")
        String lastname,
        @NotNull(message = "The email is required!")
        @NotBlank(message = "The email is required!")
        @Email(message = "The email must be correctly formatted!")
        String email
) {
}
