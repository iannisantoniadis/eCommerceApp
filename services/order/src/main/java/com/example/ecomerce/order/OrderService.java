package com.example.ecomerce.order;

import com.example.ecomerce.customer.CustomerClient;
import com.example.ecomerce.exception.BusinessException;
import com.example.ecomerce.kafka.OrderConfirmation;
import com.example.ecomerce.kafka.OrderProducer;
import com.example.ecomerce.orderLine.OrderLineRequest;
import com.example.ecomerce.orderLine.OrderLineService;
import com.example.ecomerce.payment.PaymentClient;
import com.example.ecomerce.payment.PaymentRequest;
import com.example.ecomerce.product.ProductClient;
import com.example.ecomerce.product.PurchaseResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;

    private final OrderMapper mapper;

    private final CustomerClient customerClient;

    private final ProductClient productClient;

    private final OrderLineService orderLineService;

    private final OrderProducer orderProducer;

    private final PaymentClient paymentClient;

    public Long createOrder(OrderRequest request) {
        //Check customer --> OpenFeign
        var customer = customerClient.findCustomerById(request.customerId())
                .orElseThrow(() -> new BusinessException("Cannot create order; No Customer exists with this id: " + request.customerId()));
        //Purchase the products --> product microservice
        var purchasedProducts =  productClient.purchaseProducts(request.products());
        BigDecimal total = purchasedProducts.stream().map(PurchaseResponse::price).reduce(BigDecimal.ZERO, BigDecimal::add);

        //Persist order
        var order = repository.save(mapper.toOrder(request, total));

        //Persist order lines
        orderLineService.saveOrderLines(request.products().stream().map(
                prod -> new OrderLineRequest(
                        null,
                        order.getId(),
                        prod.productId(),
                        prod.quantity()
                )).toList());


        //Start payment process
        if (!EnumUtils.isValidEnum(PaymentMethodEnum.class, request.paymentMethod())) {
            throw new BadRequestException("This payment method is unknown: " + request.paymentMethod());
        }

        var paymentRequest = new PaymentRequest(
                total,
                PaymentMethodEnum.valueOf(request.paymentMethod()),
                order.getId(),
                order.getReference(),
                customer
        );
        paymentClient.requestOrderPayment(paymentRequest);

        //Send the order confirmation to notification microservice (kafka)

        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        total,
                        PaymentMethodEnum.valueOf(request.paymentMethod()),
                        customer,
                        purchasedProducts
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
}
