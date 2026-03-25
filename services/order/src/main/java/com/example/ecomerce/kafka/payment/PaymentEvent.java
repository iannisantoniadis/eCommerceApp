package com.example.ecomerce.kafka.payment;

import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.order.PaymentMethodEnum;

import java.math.BigDecimal;

public record PaymentEvent(
        BigDecimal amount,
        PaymentMethodEnum paymentMethod,
        Long orderId,
        String orderReference,
        CustomerResponse customer
) {
}
