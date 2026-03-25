package com.example.ecomerce.order;

import com.example.ecomerce.customer.CustomerClientService;
import com.example.ecomerce.customer.CustomerResponse;
import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.OrderProducer;
import com.example.ecomerce.orderLine.OrderLineRequest;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.kafka.payment.PaymentEvent;
import com.example.ecomerce.payment.PaymentProducer;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;

    private final OrderMapper mapper;

    private final CustomerClientService customerClient;

    private final ProductClient productClient;

    private final OrderLineService orderLineService;

    private final OrderProducer orderProducer;

    private final PaymentProducer paymentProducer;

    public Long createOrder(OrderRequest request) {
        //Check customer --> OpenFeign
        var customer = customerClient.findCustomerById(request.customerId());
        //Purchase the products --> product microservice
        var purchasedProducts =  productClient.purchaseProductsAsync(request.products());

        CompletableFuture.allOf(customer,purchasedProducts).join();

        // results
        CustomerResponse customerResponse = customer.join();
        List<PurchaseResponse> purchaseResponseList = purchasedProducts.join();

        BigDecimal total = purchaseResponseList.stream()
                .map(prod -> prod.price().multiply(BigDecimal.valueOf(prod.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //Persist order
        var order = repository.save(mapper.toOrder(request, total, OrderStatusEnum.CONFIRMED));

        //Persist order lines
        orderLineService.saveOrderLines(purchaseResponseList.stream().map(
                prod -> new OrderLineRequest(
                        null,
                        prod.price(),
                        order.getId(),
                        prod.productId(),
                        prod.quantity()
                )).toList());

        //Start payment process
        PaymentMethodEnum paymentMethod = PaymentMethodEnum.valueOf(request.paymentMethod());

        var paymentEvent = new PaymentEvent(
                total,
                paymentMethod,
                order.getId(),
                order.getReference(),
                customerResponse
        );

        // nefolosit downstream
//        paymentClient.requestOrderPayment(paymentRequest).join();
        // ORDER SERV --(PaymentRequest) kafka_topic:
        paymentProducer.sendPayment(paymentEvent);


        //Send the order confirmation to notification microservice (kafka)

        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        total,
                        paymentMethod,
                        customerResponse,
                        purchaseResponseList
                )
        );
        return order.getId();
    }

    public List<OrderResponse> findAll() {
        return repository.findAll().stream().map(mapper::toOrderResponse).toList();
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
