package com.example.ecomerce.customer;

import com.example.ecomerce.exception.CustomerBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;

    private final CustomerMapper mapper;
    public String createCustomer(CustomerRequest request) {
        if (repository.findByEmail(request.email()).isPresent()){
            throw new CustomerBusinessException("This email is already in use: " + request.email());
        }
        Customer customer = repository.save(mapper.toCustomer(request));
        return customer.getId();
    }

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

    public List<CustomerResponse> findAllCustomers() {
        return repository.findAll().stream().map(mapper::toCustomerResponse).toList();
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
