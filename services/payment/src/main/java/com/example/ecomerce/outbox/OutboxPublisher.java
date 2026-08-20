package com.example.ecomerce.outbox;

import com.example.ecomerce.kafka.payment.PaymentFailureEvent;
import com.example.ecomerce.kafka.payment.PaymentSuccessEvent;
import com.example.ecomerce.payment.PaymentFailureProducer;
import com.example.ecomerce.payment.PaymentSuccessProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;

    private final PaymentSuccessProducer paymentSuccessProducer;

    private final PaymentFailureProducer paymentFailureProducer;

    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event: events){
            try {
                switch (event.getEventType()){
                    case OutboxEventTypes.PAYMENT_SUCCESS -> paymentSuccessProducer.sendPaymentSuccess(
                            objectMapper.readValue(event.getPayload(), PaymentSuccessEvent.class));
                    case OutboxEventTypes.PAYMENT_FAILURE -> paymentFailureProducer.sendPaymentFailure(
                            objectMapper.readValue(event.getPayload(), PaymentFailureEvent.class));
                }
                event.setPublished(true);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e); // after publishing failure, it is eligible to be sent again later
            }

        }
    }

}
