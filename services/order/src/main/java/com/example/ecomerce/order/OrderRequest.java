package com.example.ecomerce.order;

import com.example.ecomerce.customAnnotations.EnumValidator;
import com.example.ecomerce.product.PurchaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        String reference,
        @NotNull(message = "Payment method should be precised!")
        @EnumValidator(enumClass = PaymentMethodEnum.class, message = "This payment method is unknown!")
        String paymentMethod,
        @NotNull(message = "Customer should be set")
        @NotEmpty(message = "Customer should be set")
        @NotBlank(message = "Customer should be set")
        String customerId,
        @NotEmpty(message = "At least one product must be purchased!")
        List<PurchaseRequest> products
) {
}
