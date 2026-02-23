package com.example.ecomerce.orderLine;

import java.math.BigDecimal;

public record OrderLineRequest(
        Long id,
        Long orderId,
        Long productId,
        Double quantity
) {
}
