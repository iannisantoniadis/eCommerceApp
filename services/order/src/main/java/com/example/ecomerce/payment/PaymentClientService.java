package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PaymentClientService {

    private final PaymentClient paymentClient;

    @Async("virtualThreadExecutor")
    public CompletableFuture<Long> requestOrderPayment(PaymentEvent request) {
        return CompletableFuture.completedFuture(paymentClient.requestOrderPayment(request));
    }
}
