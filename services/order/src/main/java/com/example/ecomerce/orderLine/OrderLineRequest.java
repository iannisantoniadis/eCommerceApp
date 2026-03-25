package com.example.ecomerce.orderLine;

import java.math.BigDecimal;

public record OrderLineRequest(
        Long id,
        BigDecimal unitPrice,
        Long orderId,
        Long productId,
        Double quantity
) {
}
