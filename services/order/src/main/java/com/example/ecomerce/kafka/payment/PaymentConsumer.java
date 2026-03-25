package com.example.ecomerce.kafka.payment;

import com.example.ecomerce.order.OrderService;
import com.example.ecomerce.order.OrderStatusEnum;
import com.example.ecomerce.orderLine.OrderLine;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final OrderService orderService;

    private final OrderLineService orderLineService;

    private final ProductClient productClient;

    @KafkaListener(topics = "${spring.kafka.template.payment-success-topic}", groupId = "orderGroup")
    public void consumePaymentSuccess(PaymentSuccessEvent event) {
        log.info(String.format("Successful payment for order %s, referenced %s", event.orderId(), event.orderReference()));
        orderService.updateOrderStatus(event.orderId(), OrderStatusEnum.COMPLETED);

    }

    @KafkaListener(topics = "${spring.kafka.template..payment-failure-topic}", groupId = "orderGroup")
    public void consumePaymentFailure(PaymentFailureEvent event) {
        log.info(String.format("Failed payment for order %s, referenced %s", event.orderId(), event.orderReference()), event.reason());
        orderService.updateOrderStatus(event.orderId(), OrderStatusEnum.FAILED);
        try {
            List<OrderLine> orderLines = orderLineService.findAllByOrderId(event.orderId());
            productClient.restoreProducts(orderLines.stream()
                    .map(ol -> new PurchaseRequest(ol.getProductId(), ol.getQuantity())).toList());
        } catch (Exception e) {
            log.error("ITEM RESTORATION FAILED FOR ORDER ID: " + event.orderId() + ", MANUAL INTERVENTION IS REQUIRED! \n" + e);
        }
    }
}
