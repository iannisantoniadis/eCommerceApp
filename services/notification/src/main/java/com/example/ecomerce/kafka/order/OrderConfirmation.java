package com.example.ecomerce.kafka.order;

import com.example.ecomerce.kafka.payment.PaymentMethodEnum;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethodEnum paymentMethod,
        Customer customer,
        List<Product> products
) {
}
