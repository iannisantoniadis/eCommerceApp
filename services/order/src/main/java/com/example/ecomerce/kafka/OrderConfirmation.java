package com.example.ecomerce.kafka;

import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.order.PaymentMethodEnum;
import com.example.ecomerce.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethodEnum paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {
}
