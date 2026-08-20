package com.example.ecomerce.kafka.payment;

import com.example.ecomerce.order.OrderService;
import com.example.ecomerce.order.OrderStatusEnum;
import com.example.ecomerce.payment.PaymentTypes;
import com.example.ecomerce.processedEvent.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentEventProcessor {

    private final ProcessedEventService processedEventService;

    private final OrderService orderService;

    @Transactional
    public void processSuccessfulPayment(PaymentSuccessEvent event) {
        orderService.updateOrderStatus(event.orderId(), OrderStatusEnum.COMPLETED);
        processedEventService.markProcessed(event.orderId(), PaymentTypes.PAYMENT_SUCCESS);
    }
}
