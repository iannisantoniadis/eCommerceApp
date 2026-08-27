package com.example.ecomerce;

import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.kafka.payment.PaymentFailureEvent;
import com.example.ecomerce.kafka.payment.PaymentSuccessEvent;
import com.example.ecomerce.notification.NotificationProducer;
import com.example.ecomerce.outbox.OutboxEventTypes;
import com.example.ecomerce.outbox.OutboxService;
import com.example.ecomerce.payment.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock private PaymentRepository repository;
    @Mock private PaymentMapper mapper;
    @Mock private OutboxService outboxService;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks
    private PaymentService paymentService;


    private static final BigDecimal AMOUNT = new BigDecimal("1.0");
    private static final Long ORDER_ID = 1L;
    private static final String ORDER_REFERENCE = "REF";

    private static final String CUSTOMER_ID = "00xABC";
    private static final String CUSTOMER_FN = "John";
    private static final String CUSTOMER_LN = "DOE";
    private static final String CUSTOMER_EMAIL = "johndoe@domain.com";


    private PaymentEvent buildPaymentEvent(PaymentMethodEnum paymentMethod) {
        return new PaymentEvent(
                AMOUNT,
                paymentMethod,
                ORDER_ID,
                ORDER_REFERENCE,
                buildCustomer()
        );
    }

    private Customer buildCustomer() {
        return new Customer(
                CUSTOMER_ID,
                CUSTOMER_FN,
                CUSTOMER_LN,
                CUSTOMER_EMAIL
        );
    }

    private Payment buildPayment(PaymentMethodEnum paymentMethod) {
        return Payment.builder()
                .id(1L)
                .paymentMethod(paymentMethod)
                .orderId(ORDER_ID)
                .createdDate(LocalDateTime.now())
                .amount(AMOUNT)
                .build();
    }

    @Test
    @DisplayName("create payment - payment is created successfully")
    void createPayment_SuccessfulPaymentCreation() {
        //GIVEN
        var paymentMethod = PaymentMethodEnum.CREDIT_CARD;
        var paymentEvent = buildPaymentEvent(paymentMethod);
        var payment = buildPayment(paymentMethod);


        //WHEN
        when(mapper.toPayment(paymentEvent)).thenReturn(payment);
        when(repository.saveAndFlush(any())).thenReturn(payment);

        paymentService.createPayment(paymentEvent);

        //THEN
        verify(repository, times(1)).saveAndFlush(payment);

        // to capture with captor, use a verify(). this is basically like a debug point where you extract a value from within a method
        ArgumentCaptor<PaymentSuccessEvent> captor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
        verify(outboxService).saveOutboxEvent(eq(OutboxEventTypes.PAYMENT_SUCCESS), captor.capture());

        assertEquals(ORDER_ID, captor.getValue().orderId());
        assertEquals(ORDER_REFERENCE, captor.getValue().orderReference());

        verify(notificationProducer).sendNotification(any(PaymentEvent.class));
        verify(outboxService, never()).saveOutboxEvent(eq(OutboxEventTypes.PAYMENT_FAILURE),  any());
    }

    @Test
    @DisplayName("create payment - duplicate request")
    void createPayment_duplicateRequest() {
        //GIVEN
        var paymentMethod = PaymentMethodEnum.CREDIT_CARD;
        var paymentEvent = buildPaymentEvent(paymentMethod);
        var payment = buildPayment(paymentMethod);


        //WHEN
        when(mapper.toPayment(paymentEvent)).thenReturn(payment);
        when(repository.saveAndFlush(payment)).thenThrow(DataIntegrityViolationException.class);

        paymentService.createPayment(paymentEvent);


        //THEN
        verify(repository, times(1)).saveAndFlush(payment);
        verifyNoInteractions(outboxService, notificationProducer);
    }

    @Test
    @DisplayName("createPayment - fails and sends failure event to outbox")
    void createPayment_sendsFailureEvent(){
        //GIVEN
        var paymentMethod = PaymentMethodEnum.CREDIT_CARD;
        var paymentEvent = buildPaymentEvent(paymentMethod);
        var payment = buildPayment(paymentMethod);

        //WHEN
        when(mapper.toPayment(paymentEvent)).thenReturn(payment);
        when(repository.saveAndFlush(payment)).thenThrow(new RuntimeException("Something happened!"));

        paymentService.createPayment(paymentEvent);

        //THEN
        ArgumentCaptor<PaymentFailureEvent> captor = ArgumentCaptor.forClass(PaymentFailureEvent.class);
        verify(outboxService).saveOutboxEventInNewTransaction(eq(OutboxEventTypes.PAYMENT_FAILURE), captor.capture());
        verifyNoInteractions(notificationProducer);

        assertEquals(ORDER_ID, captor.getValue().orderId());
        assertEquals(ORDER_REFERENCE, captor.getValue().orderReference());
        assertEquals("Something happened!", captor.getValue().reason());
    }

}
