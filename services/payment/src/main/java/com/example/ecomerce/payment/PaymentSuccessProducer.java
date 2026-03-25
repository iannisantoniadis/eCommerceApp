package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentSuccessEvent;
import com.example.ecomerce.notification.PaymentNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSuccessProducer {

    @Value("${spring.kafka.template.payment-success-topic}")
    private String topic;

    private final KafkaTemplate<String, PaymentSuccessEvent> template;

    public void sendPaymentSuccess(PaymentSuccessEvent request) {
        log.info("Order <{}> with status SUCCESS", request.orderId());
        Message<PaymentSuccessEvent> message = MessageBuilder
                .withPayload(request)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        template.send(message);
    }
}
