package com.example.ecomerce.kafka.payment;

import com.example.ecomerce.order.OrderService;
import com.example.ecomerce.order.OrderStatusEnum;
import com.example.ecomerce.orderLine.OrderLine;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.processedEvent.ProcessedEventService;
import com.example.ecomerce.payment.PaymentTypes;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final OrderService orderService;

    private final OrderLineService orderLineService;

    private final ProductClient productClient;

    private final ProcessedEventService processedEventService;

    private final PaymentEventProcessor processor;

    @KafkaListener(topics = "${spring.kafka.template.payment-success-topic}", groupId = "orderGroup")
    public void consumePaymentSuccess(PaymentSuccessEvent event, Acknowledgment ack) {
        log.info(String.format("Successful payment for order %s, referenced %s", event.orderId(), event.orderReference()));
//        orderService.updateOrderStatus(event.orderId(), OrderStatusEnum.COMPLETED);
//        ack.acknowledge();
        processor.processSuccessfulPayment(event);
        ack.acknowledge();

    }




//    public void consumePaymentFailure(PaymentFailureEvent event, Acknowledgment ack) {
//        log.info(String.format("Failed payment for order %s, referenced %s", event.orderId(), event.orderReference()), event.reason());
//        orderService.updateOrderStatus(event.orderId(), OrderStatusEnum.FAILED);
//        try {
//            List<OrderLine> orderLines = orderLineService.findAllByOrderId(event.orderId());
//            productClient.restoreProducts(orderLines.stream()
//                    .map(ol -> new PurchaseRequest(ol.getProductId(), ol.getQuantity())).toList());
//        } catch (Exception e) {
//            log.error("ITEM RESTORATION FAILED FOR ORDER ID: " + event.orderId() + ", MANUAL INTERVENTION IS REQUIRED! \n" + e);
//        }
//    }

    @KafkaListener(topics = "${spring.kafka.template.payment-failure-topic}", groupId = "orderGroup")
    public void consumePaymentFailure(PaymentFailureEvent event, Acknowledgment ack) {
        log.info(String.format("Failed payment for order %s, referenced %s", event.orderId(), event.orderReference()), event.reason());

        if (processedEventService.alreadyProcessed(event.orderId(), PaymentTypes.PAYMENT_FAILURE)) {
            log.info("Payment failure for order {}, skipping", event.orderId());
            ack.acknowledge();
            return;
        }

        orderService.updateOrderStatus(event.orderId(), OrderStatusEnum.FAILED);

        try {
            List<OrderLine> orderLines = orderLineService.findAllByOrderId(event.orderId());
            productClient.restoreProducts(orderLines.stream()
                    .map(ol -> new PurchaseRequest(ol.getProductId(), ol.getQuantity())).toList());
            processedEventService.markProcessed(event.orderId(), PaymentTypes.PAYMENT_FAILURE);
        } catch (Exception e){
            // Manual intervention is needed here
            log.error("ITEM RESTORATION FAILED FOR ORDER ID: " + event.orderId() + ", MANUAL INTERVENTION IS REQUIRED! \n" + e);
        }

        // either way acknowledge the message, if successful - dealt with, if failed - needs human attention
        ack.acknowledge();

    }
}
