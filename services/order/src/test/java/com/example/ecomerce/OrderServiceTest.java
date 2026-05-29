package com.example.ecomerce;

import com.example.ecomerce.customer.CustomerClientService;
import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.exception.BusinessException;
import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.OrderProducer;
import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.order.*;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.payment.PaymentProducer;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseRequest;
import com.example.ecomerce.product.PurchaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock private CustomerClientService customerClient;
    @Mock private ProductClient productClient;
    @Mock private OrderRepository repository;
    @Mock private OrderMapper mapper;
    @Mock private OrderLineService orderLineService;
    @Mock private OrderProducer orderProducer;
    @Mock private PaymentProducer paymentProducer;

    @InjectMocks
    private OrderService orderService;

    // Shared test data
    private static final String CUSTOMER_ID = "6976768e6c275b76d8d7e78e";
    private static final Long PRODUCT_ID = 1L;
    private static final Long ORDER_ID = 11L;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100");
    private static final double QUANTITY = 1d;

    private OrderRequest buildOrderRequest(String paymentMethod) {
        return new OrderRequest(
                "REF-001",
                paymentMethod,
                CUSTOMER_ID,
                List.of(new PurchaseRequest(PRODUCT_ID, QUANTITY))
        );
    }

    private CustomerResponse buildCustomer() {
        return new CustomerResponse(CUSTOMER_ID, "FirstName", "LastName", "email@domain.com");
    }

    private PurchaseResponse buildPurchaseResponse() {
        return new PurchaseResponse(PRODUCT_ID, "Product", "Description", UNIT_PRICE, QUANTITY);
    }

    private Order buildOrder() {
        var order = new Order();
        order.setId(ORDER_ID);
        order.setReference("REF-001");
        return order;
    }

    @Test
    @DisplayName("createOrder - success: returns order id, saves order lines, sends kafka events")
    void createOrder_Success(){
        //GIVEN
        var request = buildOrderRequest("CREDIT_CARD");
        var customer = buildCustomer();
        var purchaseResponse = buildPurchaseResponse();
        var order = buildOrder();
        var expectedTotal = UNIT_PRICE.multiply(BigDecimal.valueOf(QUANTITY)); // 100 * 1 = 100

        //WHEN
        when(customerClient.findCustomerById(CUSTOMER_ID))
                .thenReturn(CompletableFuture.completedFuture(customer));
        when(productClient.purchaseProductsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(purchaseResponse)));
        when(mapper.toOrder(request, expectedTotal, OrderStatusEnum.PENDING))
                .thenReturn(order);
        when(repository.save(any()))
                .thenReturn(order);

        Long orderId = orderService.createOrder(request);

        //THEN
        assertNotNull(orderId);
        assertEquals(ORDER_ID, orderId);

        // Verify order total calculation
        verify(mapper).toOrder(request, expectedTotal, OrderStatusEnum.PENDING);

        // Verify order lines persisted
        verify(orderLineService, times(1)).saveOrderLines(anyList());

        // Verify both kafka events fired
        verify(paymentProducer, times(1)).sendPayment(any(PaymentEvent.class));
        verify(orderProducer, times(1)).sendOrderConfirmation(any(OrderConfirmation.class));
    }

    @Test
    @DisplayName("createOrder - success: total is correctly calculated from multiple products")
    void createOrder_TotalCalculation() {
        // GIVEN — two products: 100 x 2 + 50 x 3 = 350
        var request = new OrderRequest(
                "REF-002",
                "CREDIT_CARD",
                CUSTOMER_ID,
                List.of(
                        new PurchaseRequest(1L, 2d),
                        new PurchaseRequest(2L, 3d)
                )
        );

        var purchaseResponses = List.of(
                new PurchaseResponse(1L, "Product A", "Desc", new BigDecimal("100"), 2d),
                new PurchaseResponse(2L, "Product B", "Desc", new BigDecimal("50"), 3d)
        );

        var expectedTotal = new BigDecimal("350.0"); // 100*2 + 50*3
        var order = buildOrder();

        when(customerClient.findCustomerById(CUSTOMER_ID))
                .thenReturn(CompletableFuture.completedFuture(buildCustomer()));
        when(productClient.purchaseProductsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(purchaseResponses));
        when(mapper.toOrder(request, expectedTotal, OrderStatusEnum.PENDING))
                .thenReturn(order);
        when(repository.save(any()))
                .thenReturn(order);

        // WHEN
        Long orderId = orderService.createOrder(request);

        // THEN
        assertNotNull(orderId);
        // Verify the total passed to mapper is correct
        verify(mapper).toOrder(request, expectedTotal, OrderStatusEnum.PENDING);
    }

    @Test
    @DisplayName("createOrder - customer service failure propagates as BusinessException")
    void createOrder_CustomerServiceFails() {
        // GIVEN — customer service throws an exception
        when(customerClient.findCustomerById(CUSTOMER_ID))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Customer service unavailable")
                ));
        when(productClient.purchaseProductsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(buildPurchaseResponse())));

        // WHEN & THEN
        // CompletableFuture.allOf().join() wraps exceptions in CompletionException
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(buildOrderRequest("CREDIT_CARD")));

        // Nothing should be persisted or sent
        verifyNoInteractions(repository, orderLineService, orderProducer, paymentProducer);
    }

    @Test
    @DisplayName("createOrder - product service failure propagates as BusinessException")
    void createOrder_ProductServiceFails() {
        // GIVEN — product service throws (e.g. insufficient stock)
        when(customerClient.findCustomerById(CUSTOMER_ID))
                .thenReturn(CompletableFuture.completedFuture(buildCustomer()));
        when(productClient.purchaseProductsAsync(any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Insufficient stock")
                ));

        // WHEN & THEN
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(buildOrderRequest("CREDIT_CARD")));

        verifyNoInteractions(repository, orderLineService, orderProducer, paymentProducer);
    }

    @Test
    @DisplayName("createOrder - order lines are built correctly from purchase responses")
    void createOrder_OrderLinesBuiltCorrectly() {
        // GIVEN
        var request = buildOrderRequest("CREDIT_CARD");
        var purchaseResponse = buildPurchaseResponse();
        var order = buildOrder();

        when(customerClient.findCustomerById(CUSTOMER_ID))
                .thenReturn(CompletableFuture.completedFuture(buildCustomer()));
        when(productClient.purchaseProductsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(purchaseResponse)));
        when(mapper.toOrder(any(), any(), any()))
                .thenReturn(order);
        when(repository.save(any()))
                .thenReturn(order);

        // WHEN
        orderService.createOrder(request);

        // THEN — capture and verify the order lines passed to the service
        verify(orderLineService).saveOrderLines(argThat(lines -> {
            assertEquals(1, lines.size());
            var line = lines.getFirst();
            assertEquals(ORDER_ID, line.orderId());
            assertEquals(PRODUCT_ID, line.productId());
            assertEquals(UNIT_PRICE, line.unitPrice());
            assertEquals(QUANTITY, line.quantity());
            return true;
        }));
    }

    @Test
    @DisplayName("createOrder - invalid payment method throws IllegalArgumentException at service layer")
    void createOrder_InvalidPaymentMethod() {
        when(customerClient.findCustomerById(CUSTOMER_ID))
                .thenReturn(CompletableFuture.completedFuture(buildCustomer()));
        when(productClient.purchaseProductsAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(List.of(buildPurchaseResponse())));
        when(mapper.toOrder(any(), any(), any()))
                .thenReturn(buildOrder());
        when(repository.save(any()))
                .thenReturn(buildOrder());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(buildOrderRequest("INVALID_METHOD")));

        verify(repository, times(1)).save(any());
        verifyNoInteractions(paymentProducer, orderProducer);
    }

    @Test
    @DisplayName("findById - returns mapped response for existing order")
    void findById_Success() {
        // GIVEN
        var order = buildOrder();
        var expectedResponse = new OrderResponse(ORDER_ID, "REF-001", BigDecimal.TEN,
                PaymentMethodEnum.CREDIT_CARD, OrderStatusEnum.PENDING, CUSTOMER_ID);

        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(mapper.toOrderResponse(order)).thenReturn(expectedResponse);

        // WHEN
        var result = orderService.findById(ORDER_ID);

        // THEN
        assertNotNull(result);
        assertEquals(ORDER_ID, result.id());
        assertEquals("REF-001", result.reference());
    }

    @Test
    @DisplayName("findAll - returns all mapped orders")
    void findAll_ReturnsMappedList() {
        // GIVEN
        var order = buildOrder();
        var response = new OrderResponse(ORDER_ID, "REF-001", BigDecimal.TEN,
                PaymentMethodEnum.CREDIT_CARD, OrderStatusEnum.PENDING, CUSTOMER_ID);

        int page = 1;
        int size = 20;
        var orderPage = new PageImpl<>(List.of(order));
        when(repository.findAll(PageRequest.of(page, size, Sort.by("id")))).thenReturn(orderPage);
        when(mapper.toOrderResponse(order)).thenReturn(response);

        // WHEN
        var results = orderService.findAll(page, size);

        // THEN
        assertEquals(1, results.size());
        assertEquals(ORDER_ID, results.getFirst().id());
    }
}
