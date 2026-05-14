package com.example.ecomerce.customer;

import com.example.ecomerce.exception.CustomerBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;

    private final CustomerMapper mapper;
    public String createCustomer(CustomerRequest request) {
        Customer customer = repository.save(mapper.toCustomer(request));
        return customer.getId();
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public void updateCustomer(CustomerRequest request) {
        var customer = repository.findById(request.id()).orElseThrow(() ->
                new CustomerBusinessException(String.format("No customer found for the provided id: %s", request.id())));
        mergerCustomer(customer, request);
        repository.save(customer);

    }

    private void mergerCustomer(Customer customer, CustomerRequest request) {
        if (StringUtils.hasLength(request.firstname()))
            customer.setFirstname(request.firstname());
        if (StringUtils.hasLength(request.lastname()))
            customer.setLastname(request.lastname());
        if (StringUtils.hasLength(request.email()))
            customer.setEmail(request.email());
        if (request.address() != null)
            customer.setAddress(request.address());
    }

    public Page<CustomerResponse> findAllCustomers(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).map(mapper::toCustomerResponse);
    }

    public Optional<Customer> findById(String customerId) {
        return repository.findById(customerId);
    }

    public CustomerResponse findByIdResponse(String customerId){
        return mapper.toCustomerResponse(findById(customerId).orElseThrow(() ->
                new CustomerBusinessException(String.format("No customer found for the provided id: %s", customerId))));
    }

    public void deleteById(String customerId) {
        repository.deleteById(customerId);
    }
}
