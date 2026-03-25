package com.example.ecomerce.order;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        String reference,
        BigDecimal amount,
        PaymentMethodEnum paymentMethod,
        OrderStatusEnum status,
        String customerId
) {
}
