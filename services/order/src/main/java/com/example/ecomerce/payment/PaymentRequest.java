package com.example.ecomerce.payment;

import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.order.PaymentMethodEnum;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethodEnum paymentMethod,
        Long orderId,
        String orderReference,
        CustomerResponse customer
) {
}
