package com.example.ecomerce.order;

import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.orderLine.OrderLineRequest;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.outbox.OutboxEventTypes;
import com.example.ecomerce.outbox.OutboxService;
import com.example.ecomerce.product.PurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceTransactional {

    private final OrderRepository repository;

    private final OrderMapper mapper;

    private final OrderLineService orderLineService;

    private final OutboxService outboxService;


    /**
     * The purpose of this method is to separate that which needs to be atomically processed and saved from that which uses circuit-breaker.
     * Were the whole previous method to be wrapped under one singular transaction, waiting for the circuit-breaker to flip would exhaust
     * the connection pool to the DB, as such the two parts have now been separated.
     */
    @Transactional
    public Long persistOrderAndEnqueueEvents(OrderRequest request, BigDecimal total, CustomerResponse customerResponse,
                                             List<PurchaseResponse> purchaseResponseList) {
        //Persist order
        var order = repository.save(mapper.toOrder(request, total, OrderStatusEnum.PENDING));

        //Persist order lines
        orderLineService.saveOrderLines(purchaseResponseList.stream().map(
                prod -> new OrderLineRequest(
                        null,
                        prod.price(),
                        order.getId(),
                        prod.productId(),
                        prod.quantity()
                )).toList());

        PaymentMethodEnum paymentMethod = PaymentMethodEnum.valueOf(request.paymentMethod());

        var paymentEvent = new PaymentEvent(
                total,
                paymentMethod,
                order.getId(),
                order.getReference(),
                customerResponse
        );

        // ORDER SERV --(PaymentRequest) kafka_topic:
        outboxService.saveOutboxEvent(OutboxEventTypes.PAYMENT_REQUEST, paymentEvent);

        //Send the order confirmation to notification microservice (kafka)
        // update: For now I don't care that much about idempotence for this service, it has been acknowledged and placed as low priority


        outboxService.saveOutboxEvent(OutboxEventTypes.ORDER_CONFIRMATION,
                new OrderConfirmation(
                        request.reference(),
                        total,
                        paymentMethod,
                        customerResponse,
                        purchaseResponseList
                ));
        return order.getId();
    }
}
