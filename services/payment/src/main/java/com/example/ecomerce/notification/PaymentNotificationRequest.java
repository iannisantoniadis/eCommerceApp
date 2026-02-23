package com.example.ecomerce.notification;

import com.example.ecomerce.payment.PaymentMethodEnum;

import java.math.BigDecimal;

public record PaymentNotificationRequest(
        String orderReference,
        BigDecimal amount,
        PaymentMethodEnum paymentMethod,
        String customerFirstname,
        String customerLastname,
        String customerEmail
) {
}
