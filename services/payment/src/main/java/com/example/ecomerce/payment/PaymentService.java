package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.kafka.payment.PaymentFailureEvent;
import com.example.ecomerce.kafka.payment.PaymentSuccessEvent;
import com.example.ecomerce.notification.NotificationProducer;
import com.example.ecomerce.notification.PaymentNotificationRequest;
import com.example.ecomerce.outbox.OutboxEvent;
import com.example.ecomerce.outbox.OutboxEventTypes;
import com.example.ecomerce.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;

    private final PaymentMapper mapper;

    private final OutboxService outboxService;

    private final NotificationProducer notificationProducer;

    @Transactional
    public void createPayment(PaymentEvent request) {
        //Idempotence check - not by check-and-insert, but atomically, at DB level
        try {
            // saveAndFlush() throws exceptions here, whereas save() would throw them at the end of the transaction. where they are no longer handled (as in no longer inside the try-catch)
            repository.saveAndFlush(mapper.toPayment(request));
        }
        catch (DataIntegrityViolationException e) {
            log.info("Payment was already processed for order {}, skipping", request.orderId());
            return; // If I already processed it before, no need to do so again, now
        }
        catch (Exception e){
            log.error("Payment failed for order {}", request.orderId(), e);
            // Save event failure
            // purpose of this method is to use a new transaction as the one currently here might be compromised depending on the unexpected nature of the exception,
            // simply put, I want atomicity between the save and the success, the failure is independent of the current transaction
            outboxService.saveOutboxEventInNewTransaction(OutboxEventTypes.PAYMENT_FAILURE, new PaymentFailureEvent(request.orderId(), request.orderReference(), e.getMessage()));
            return;
        }

        outboxService.saveOutboxEvent(OutboxEventTypes.PAYMENT_SUCCESS,
                new PaymentSuccessEvent(request.orderId(), request.orderReference()));

        //notifications outside SAGA
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
    }
}
