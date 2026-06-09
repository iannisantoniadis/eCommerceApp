package com.example.ecomerce.orderLine;

import com.example.ecomerce.order.Order;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;

@Service
public class OrderLineMapper {
    public OrderLine toOrderLine(OrderLineRequest orderLineRequest) {
        return OrderLine.builder()
                .id(orderLineRequest.id())
                .quantity(orderLineRequest.quantity())
                .unitPrice(orderLineRequest.unitPrice())
                .order(
                        Order.builder()
                        .id(orderLineRequest.orderId())
                        .version(0L)
                        .build()
                )
                .productId(orderLineRequest.productId())
                .build();
    }

    public OrderLineResponse toOrderLineResponse(OrderLine orderLine) {
        return new OrderLineResponse(
                orderLine.getId(),
                orderLine.getQuantity()
        );
    }
}
