package com.example.ecomerce.outbox;

import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.OrderProducer;
import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.payment.PaymentProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final ObjectMapper objectMapper;

    private final OutboxService outboxService;

    private final PaymentProducer paymentProducer;

    private final OrderProducer orderProducer;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingOrdersToPayment() {

        //find top 50 unpublished outbox events for Payment and publish them
        List<OutboxEvent> unpublishedList = outboxService.findUnpublishedPaymentConfirmation();

        for (OutboxEvent event : unpublishedList){
            try {
                paymentProducer.sendPayment(objectMapper.readValue(event.getPayload(), PaymentEvent.class));
                event.setPublished(true);
                outboxService.save(event);
            }  catch (Exception e) {
                log.error("Failed to publish outbox event {} to Payment", event.getId(), e); // after publishing failure, it is eligible to be sent again later
            }
        }
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingOrdersToNotification() {

        //find top 50 unpublished outbox events for Notification and publish them
        List<OutboxEvent> unpublishedList = outboxService.findUnpublishedOrdersForNotification();

        for (OutboxEvent event : unpublishedList){
            try {
                orderProducer.sendOrderConfirmation(objectMapper.readValue(event.getPayload(), OrderConfirmation.class));
                event.setPublished(true);
                outboxService.save(event);
            }  catch (Exception e) {
                log.error("Failed to publish outbox event {} to Notification", event.getId(), e); // after publishing failure, it is eligible to be sent again later
            }
        }
    }
}
