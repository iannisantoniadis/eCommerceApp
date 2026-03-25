package com.example.ecomerce.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(
        name = "customer-service",
        url = "${application.config.customer-url}",
        configuration = CustomerFeignConfig.class
)
public interface CustomerClient {

    @GetMapping("/{customer-id}")
    CustomerResponse findCustomerById(
            @PathVariable("customer-id") String customerId
    );
}
