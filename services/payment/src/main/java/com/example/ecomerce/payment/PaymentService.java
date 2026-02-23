package com.example.ecomerce.payment;

import com.example.ecomerce.notification.NotificationProducer;
import com.example.ecomerce.notification.PaymentNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;

    private final PaymentMapper mapper;

    private final NotificationProducer notificationProducer;
    public Long createPayment(PaymentRequest request) {
        var paymentId = repository.save(mapper.toPayment(request)).getId();
        notificationProducer.sendNotification(
                new PaymentNotificationRequest(
                        request.orderReference(),
                        request.amount(),
                        request.paymentMethod(),
                        request.customer().firstname(),
                        request.customer().lastname(),
                        request.customer().email())
        );
        return paymentId;
    }
}
