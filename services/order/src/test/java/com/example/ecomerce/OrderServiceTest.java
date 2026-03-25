package com.example.ecomerce;

import com.example.ecomerce.customer.CustomerClient;
import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.exception.BusinessException;
import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.OrderProducer;
import com.example.ecomerce.order.*;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseRequest;
import com.example.ecomerce.product.PurchaseResponse;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock private CustomerClient customerClient;
    @Mock private ProductClient productClient;
    @Mock private OrderRepository repository;
    @Mock private OrderMapper mapper;
    @Mock private OrderLineService orderLineService;
//    @Mock private PaymentClient paymentClient;
    @Mock private OrderProducer orderProducer;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Successfully create order")
    void createOrder_Success(){
        // GIVEN
        var customerId = "6976768e6c275b76d8d7e78e";
        var productId = 1L;
        var purchaseRequest = new PurchaseRequest(productId, 1d);
        var orderRequest = new OrderRequest("REF-001", "CREDIT_CARD", customerId, List.of(purchaseRequest));

        var customer = new CustomerResponse(customerId, "FirstName", "LastName", "email@domain.com");
        var purchaseResponse = new PurchaseResponse(productId, "Product whatever", "Description whatever", new BigDecimal(100), 1d);
        var orderEntity = new Order();
        orderEntity.setId(11L);
        orderEntity.setReference("REF-001");
        var total = new BigDecimal(100);

        //MOCKING
        when(customerClient.findCustomerById(orderRequest.customerId())).thenReturn(customer);
        when(productClient.purchaseProductsAsync(any())).thenReturn(CompletableFuture.completedFuture(List.of(purchaseResponse)));
        when(mapper.toOrder(orderRequest, total, any(OrderStatusEnum.class))).thenReturn(orderEntity);
        when(repository.save(any())).thenReturn(orderEntity);

        //WHEN
        Long orderId = orderService.createOrder(orderRequest);

        //THEN
        assertNotNull(orderId);
        assertEquals(11L, orderId);

        //CHECK CALLS
        verify(orderLineService, times(1)).saveOrderLines(anyList());
//        verify(paymentClient, times(1)).requestOrderPayment(any(PaymentEvent.class));
//        verify(paymentClient, times(1)).requestOrderPayment(any(PaymentEvent.class));
        verify(orderProducer, times(1)).sendOrderConfirmation(any(OrderConfirmation.class));

        //CHECK PRICE
        verify(mapper).toOrder(orderRequest, total, OrderStatusEnum.CONFIRMED);
    }

    @Test
    @DisplayName("Throws BusinessException when customer does not exist")
    void createOrder_CustomerNotFound() {
        // GIVEN
        var customerId = "invalid-id";
        var productId = 1L;
        var purchaseRequest = new PurchaseRequest(productId, 1d);
        var orderRequest = new OrderRequest("REF-001", "CREDIT_CARD", customerId, List.of(purchaseRequest));
        when(customerClient.findCustomerById("invalid-id")).thenReturn(null);

        // WHEN & THEN
        var exception = assertThrows(BusinessException.class, () -> orderService.createOrder(orderRequest));
        assertTrue(exception.getMessage().contains("No Customer exists"));

        // Verify we stop early
        verifyNoInteractions(productClient, repository, orderProducer);
    }

    @Test
    @DisplayName("Throws BadRequestException for invalid payment method")
    void createOrder_InvalidPaymentMethod() {
        // GIVEN
        var customerId = "6976768e6c275b76d8d7e78e";
        var productId = 1L;
        var purchaseRequest = new PurchaseRequest(productId, 1d);
        var orderRequest = new OrderRequest("REF-001", "invalid_type", customerId, List.of(purchaseRequest));
        var customer = new CustomerResponse(customerId, "FirstName", "LastName", "email@domain.com");

        when(customerClient.findCustomerById(any())).thenReturn(customer);
        when(productClient.purchaseProductsAsync(any())).thenReturn(CompletableFuture.completedFuture(List.of()));
        when(repository.save(any())).thenReturn(new Order());

        // WHEN & THEN
        assertThrows(BadRequestException.class, () -> orderService.createOrder(orderRequest));
//        verifyNoInteractions(paymentClient, orderProducer);
    }
}
