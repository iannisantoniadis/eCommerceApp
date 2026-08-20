package com.example.ecomerce.order;

import com.example.ecomerce.customer.CustomerClientService;
import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.exception.BusinessException;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseRequest;
import com.example.ecomerce.product.PurchaseResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final OrderRepository repository;

    private final OrderServiceTransactional orderServiceTransactional;

    private final OrderMapper mapper;

    private final CustomerClientService customerClient;

    private final ProductClient productClient;

    private CircuitBreaker customerCB() {
        return circuitBreakerRegistry.circuitBreaker("customerService");
    }

    private CircuitBreaker productCB() {
        return circuitBreakerRegistry.circuitBreaker("productService");
    }

    public Long createOrder(OrderRequest request) {
        //Check customer --> OpenFeign
        Supplier<CompletableFuture<CustomerResponse>> customerSupplier =
                CircuitBreaker.decorateSupplier(customerCB(), () -> customerClient.findCustomerById(request.customerId()));
        //Purchase the products --> product microservice
        Supplier<CompletableFuture<List<PurchaseResponse>>> productSupplier =
                CircuitBreaker.decorateSupplier(productCB(), () -> productClient.purchaseProductsAsync(request.products()));

        CompletableFuture<CustomerResponse> customerFuture = customerSupplier.get();
        CompletableFuture<List<PurchaseResponse>> productFuture = productSupplier.get();

        try {
            CompletableFuture.allOf(customerFuture, productFuture).join();
        }
        catch (Exception ex){
            //products were purchased but the customer was not fetched for some reason
            if (productFuture.isDone() && customerFuture.isCompletedExceptionally()){
                productClient.restoreProducts(productFuture.join()
                        .stream().map(prod -> new PurchaseRequest(prod.productId(), prod.quantity())).toList());
            }
            throw new BusinessException("Order creation failed: " + ex.getMessage());
        }

        // results
        CustomerResponse customerResponse = customerFuture.join();
        List<PurchaseResponse> purchaseResponseList = productFuture.join();

        BigDecimal total = purchaseResponseList.stream()
                .map(prod -> prod.price().multiply(BigDecimal.valueOf(prod.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return orderServiceTransactional.persistOrderAndEnqueueEvents(request, total, customerResponse, purchaseResponseList);
    }


    public List<OrderResponse> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).stream().map(mapper::toOrderResponse).toList();
    }

    public OrderResponse findById(Long orderId) {
        return repository.findById(orderId)
                .map(mapper::toOrderResponse)
                .orElseThrow(() -> new EntityNotFoundException("There is no order with id: " + orderId));
    }

    public void updateOrderStatus(Long orderId, OrderStatusEnum status) {
        repository.updateOrderStatus(orderId, status);
    }
}
