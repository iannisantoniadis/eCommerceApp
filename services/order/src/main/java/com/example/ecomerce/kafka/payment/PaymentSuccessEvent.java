package com.example.ecomerce.kafka.payment;

public record PaymentSuccessEvent(
        Long orderId,
        String orderReference
) {
}
