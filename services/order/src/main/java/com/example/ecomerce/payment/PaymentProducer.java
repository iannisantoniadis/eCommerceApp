package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentEvent;
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
public class PaymentProducer {

    @Value("${spring.kafka.template.payment-topic}")
    private String topic;

    private final KafkaTemplate<String, PaymentEvent> template;

    public void sendPayment(PaymentEvent request) {
        log.info("Sending payment with body<{}>", request);
        Message<PaymentEvent> message = MessageBuilder
                .withPayload(request)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        template.send(message);
    }
}
