package com.example.ecomerce.kafka.notificationConsumer;

import com.example.ecomerce.email.EmailService;
import com.example.ecomerce.kafka.order.OrderConfirmation;
import com.example.ecomerce.kafka.payment.PaymentConfirmation;
import com.example.ecomerce.notification.Notification;
import com.example.ecomerce.notification.NotificationRepository;
import com.example.ecomerce.notification.NotificationTypeEnum;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository repository;

    private final EmailService emailService;

    @KafkaListener(topics = "${app.kafka.topic.payment-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumePaymentSuccessNotification(PaymentConfirmation confirmation) throws MessagingException {
        log.info(String.format("Consuming the message from <<payment-topic>> Topic: %s", confirmation));
        repository.save(
                Notification
                        .builder()
                        .notificationType(NotificationTypeEnum.PAYMENT_CONFIRMATION)
                        .notificationDate(LocalDateTime.now())
                        .paymentConfirmation(confirmation)
                        .build()
        );
        var customerName = confirmation.customerFirstname() + " " + confirmation.customerLastname();
        emailService.sendPaymentSuccessEmail(
                confirmation.customerEmail(),
                customerName,
                confirmation.amount(),
                confirmation.orderReference()
        );

    }

    @KafkaListener(topics = "${app.kafka.topic.order-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderConfirmationNotification(OrderConfirmation confirmation) throws MessagingException {
        log.info(String.format("Consuming the message from <<order-topic>> Topic: %s", confirmation));
        repository.save(
                Notification
                        .builder()
                        .notificationType(NotificationTypeEnum.ORDER_CONFIRMATION)
                        .notificationDate(LocalDateTime.now())
                        .orderConfirmation(confirmation)
                        .build()
        );

        var customerName = confirmation.customer().firstname() + " " + confirmation.customer().lastname();
        emailService.sendOrderConfirmationEmail(confirmation.customer().email(),
                customerName,
                confirmation.totalAmount(),
                confirmation.orderReference(),
                confirmation.products());

    }
}
