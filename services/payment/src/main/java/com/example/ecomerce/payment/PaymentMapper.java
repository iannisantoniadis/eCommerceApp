package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentEvent;
import org.springframework.stereotype.Service;

@Service
public class PaymentMapper {

    public Payment toPayment(PaymentEvent request){
        return Payment
                .builder()
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .orderId(request.orderId())
                .build();
    }
}
