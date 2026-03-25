package com.example.ecomerce.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class CustomerClientService {

    private final CustomerClient customerClient;

    @Async("virtualThreadExecutor")
    public CompletableFuture<CustomerResponse> findCustomerById(String customerId) {
        return CompletableFuture.completedFuture(customerClient.findCustomerById(customerId));
    }

}
