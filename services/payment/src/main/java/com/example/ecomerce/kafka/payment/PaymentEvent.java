package com.example.ecomerce.kafka.payment;

import com.example.ecomerce.payment.Customer;
import com.example.ecomerce.payment.PaymentMethodEnum;

import java.math.BigDecimal;

public record PaymentEvent(
        BigDecimal amount,
        PaymentMethodEnum paymentMethod,
        Long orderId,
        String orderReference,
        Customer customer
) {
}
