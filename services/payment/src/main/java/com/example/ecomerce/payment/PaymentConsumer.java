package com.example.ecomerce.payment;


import com.example.ecomerce.kafka.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentService paymentService;


    @KafkaListener(topics = "${spring.kafka.template.payment-topic}", groupId = "paymentGroup")
    public void consumePayment(PaymentEvent request, Acknowledgment ack) {
        log.info(String.format("Consuming the payment request sent from Order through <<payment-topic>> Topic: %s", request));
        paymentService.createPayment(request);
        // This part is out of the transactional part, even if it fails right here, the orderId Unique Key constraint will
        // warn about the message being already consumed so no failure will be registered and sent back as a payment failure event
        ack.acknowledge();
    }
}
