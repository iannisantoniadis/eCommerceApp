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
//        Long paymentId = null;
        try {
//            paymentId = repository.save(mapper.toPayment(request)).getId();
            repository.save(mapper.toPayment(request));
            notificationProducer.sendNotification(
                    new PaymentNotificationRequest(
                            request.orderReference(),
                            request.amount(),
                            request.paymentMethod(),
                            request.customer().firstname(),
                            request.customer().lastname(),
                            request.customer().email())
            );
            paymentSuccessProducer.sendPaymentSuccess(new PaymentSuccessEvent(request.orderId(), request.orderReference()));
        } catch (Exception e) {
            log.error("Payment failed for order {}", request.orderId(), e);
            paymentFailureProducer.sendPaymentFailure(
                    new PaymentFailureEvent(request.orderId(), request.orderReference(), e.getMessage()));
        }
//        return paymentId;
    }
}
