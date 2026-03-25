package com.example.ecomerce.payment;

import com.example.ecomerce.kafka.payment.PaymentEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "product-service",
        url = "${application.config.payment-url}"
)
public interface PaymentClient {

    @PostMapping
    Long requestOrderPayment(@RequestBody PaymentEvent request);
}
