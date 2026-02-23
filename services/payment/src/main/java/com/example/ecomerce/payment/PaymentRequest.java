package com.example.ecomerce.payment;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethodEnum paymentMethod,
        Long orderId,
        String orderReference,
        Customer customer
) {
}
