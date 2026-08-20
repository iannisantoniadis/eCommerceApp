package com.example.ecomerce;

import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.order.*;
import com.example.ecomerce.orderLine.OrderLineRequest;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.outbox.OutboxEventTypes;
import com.example.ecomerce.outbox.OutboxService;
import com.example.ecomerce.product.PurchaseRequest;
import com.example.ecomerce.product.PurchaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTransactionalTest {

    @Mock private OrderRepository repository;
    @Mock private OrderMapper mapper;
    @Mock private OrderLineService orderLineService;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private OrderServiceTransactional orderServiceTransactional;

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
    @DisplayName("persistOrderAndEnqueueEvents - order lines are built correctly from purchase responses")
    void persistOrderAndEnqueueEvents_OrderLinesBuildCorrectly(){
        // GIVEN
        var request = buildOrderRequest("CREDIT_CARD");
        var purchaseResponse = buildPurchaseResponse();
        var customer = buildCustomer();
        var order = buildOrder();
        var total = purchaseResponse.price().multiply(BigDecimal.valueOf(purchaseResponse.quantity()));

        when(mapper.toOrder(any(), any(), any()))
                .thenReturn(order);
        when(repository.save(any()))
                .thenReturn(order);

        // WHEN
        orderServiceTransactional.persistOrderAndEnqueueEvents(
                request,
                total,
                customer,
                List.of(purchaseResponse)
                );

        // THEN — capture and verify the order lines passed to the service
        ArgumentCaptor<List<OrderLineRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderLineService).saveOrderLines(captor.capture());

        List<OrderLineRequest> orderLines = captor.getValue();
        assertEquals(1, orderLines.size());
        var line = orderLines.getFirst();
        assertEquals(ORDER_ID, line.orderId());
        assertEquals(PRODUCT_ID, line.productId());
        assertEquals(UNIT_PRICE, line.unitPrice());
        assertEquals(QUANTITY, line.quantity());
    }

    @Test
    @DisplayName("persistOrderAndEnqueueEvents - invalid payment method throws IllegalArgumentException at service layer")
    void persistOrderAndEnqueueEvents_InvalidPaymentMethod() {
        // GIVEN
        var request = buildOrderRequest("WRONG!");
        var purchaseResponse = buildPurchaseResponse();
        var customer = buildCustomer();
        var order = buildOrder();
        var total = purchaseResponse.price().multiply(BigDecimal.valueOf(purchaseResponse.quantity()));

        when(mapper.toOrder(any(), any(), any()))
                .thenReturn(order);
        when(repository.save(any()))
                .thenReturn(order);

        assertThrows(IllegalArgumentException.class,
                () -> orderServiceTransactional.persistOrderAndEnqueueEvents(
                        request,
                        total,
                        customer,
                        List.of(purchaseResponse)
                ));
        verify(repository, times(1)).save(any());
        verify(outboxService, never()).saveOutboxEvent(any(), any());
    }

    @Test
    @DisplayName("persistOrderAndEnqueueEvents - enqueues both PaymentRequest and OrderConfirmation outbox events")
    void persistOrderAndEnqueueEvents_EnqueuesOutboxEvents() {
        var request = buildOrderRequest("CREDIT_CARD");
        var purchaseResponse = buildPurchaseResponse();
        var customer = buildCustomer();
        var order = buildOrder();
        var total = purchaseResponse.price().multiply(BigDecimal.valueOf(purchaseResponse.quantity()));

        when(mapper.toOrder(any(), any(), any())).thenReturn(order);
        when(repository.save(any())).thenReturn(order);

        orderServiceTransactional.persistOrderAndEnqueueEvents(request, total, customer, List.of(purchaseResponse));

        verify(outboxService).saveOutboxEvent(eq(OutboxEventTypes.PAYMENT_REQUEST), any(PaymentEvent.class));
        verify(outboxService).saveOutboxEvent(eq(OutboxEventTypes.ORDER_CONFIRMATION), any(OrderConfirmation.class));
    }
}
