package com.example.ecomerce.kafka.payment;

public record PaymentFailureEvent(
        Long orderId,
        String orderReference,
        String reason
) {
}
