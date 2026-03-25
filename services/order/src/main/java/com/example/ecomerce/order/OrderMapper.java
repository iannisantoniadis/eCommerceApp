package com.example.ecomerce.order;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderMapper {

    public Order toOrder(OrderRequest request, BigDecimal total, OrderStatusEnum status){
        var order = toOrder(request);
        order.setTotalAmount(total);
        order.setStatus(status);
        return order;
    }

    public Order toOrder(OrderRequest request){
        return Order.builder()
                .customerId(request.customerId())
                .reference(request.reference())
                .paymentMethod(PaymentMethodEnum.valueOf(request.paymentMethod()))
                .build();
    }

    public OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getReference(),
            order.getTotalAmount(),
            order.getPaymentMethod(),
            order.getStatus(),
            order.getCustomerId()
        );
    }
}
