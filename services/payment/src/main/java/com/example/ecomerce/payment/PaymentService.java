package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.kafka.payment.PaymentFailureEvent;
import com.example.ecomerce.kafka.payment.PaymentSuccessEvent;
import com.example.ecomerce.notification.NotificationProducer;
import com.example.ecomerce.notification.PaymentNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;

    private final PaymentMapper mapper;

    private final PaymentSuccessProducer paymentSuccessProducer;

    private final PaymentFailureProducer paymentFailureProducer;

    private final NotificationProducer notificationProducer;

    public void createPayment(PaymentEvent request) {
        try {
            repository.save(mapper.toPayment(request));
        } catch (Exception e) {
            log.error("Payment failed for order {}", request.orderId(), e);
            paymentFailureProducer.sendPaymentFailure(
                    new PaymentFailureEvent(request.orderId(), request.orderReference(), e.getMessage()));
            return; // so it won't send two contrary events for the same order
        }

        try {
            notificationProducer.sendNotification(
                new PaymentNotificationRequest(
                        request.orderReference(),
                        request.amount(),
                        request.paymentMethod(),
                        request.customer().firstname(),
                        request.customer().lastname(),
                        request.customer().email())
        );
        }
        catch (Exception e) {
            log.error("Payment was successful but the notification failed for order {}", request.orderId(), e);
        }

        paymentSuccessProducer.sendPaymentSuccess(new PaymentSuccessEvent(request.orderId(), request.orderReference()));
    }
}
