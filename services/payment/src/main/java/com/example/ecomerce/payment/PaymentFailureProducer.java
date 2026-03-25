package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentFailureEvent;
import com.example.ecomerce.kafka.payment.PaymentSuccessEvent;
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
public class PaymentFailureProducer {

    @Value("${spring.kafka.template.payment-failure-topic}")
    private String topic;

    private final KafkaTemplate<String, PaymentFailureEvent> template;

    public void sendPaymentFailure(PaymentFailureEvent request) {
        log.info("Order <{}> with status FAILURE", request.orderId());
        Message<PaymentFailureEvent> message = MessageBuilder
                .withPayload(request)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        template.send(message);
    }
}
