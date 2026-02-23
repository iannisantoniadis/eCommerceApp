package com.example.ecomerce.orderLine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderLineService {

    private final OrderLineRepository repository;

    private final OrderLineMapper mapper;

    public Long saveOrderLine(OrderLineRequest orderLineRequest) {
        var order = mapper.toOrderLine(orderLineRequest);
        return repository.save(order).getId();
    }

    public List<Long> saveOrderLines(List<OrderLineRequest> orderLineRequestList) {
        return repository.saveAll(orderLineRequestList.stream().map(mapper::toOrderLine).toList())
                .stream().map(OrderLine::getId).toList();
    }

    public List<OrderLineResponse> findAllByOrderId(Long orderId) {
        return repository.findAllByOrderId(orderId)
                .stream().map(mapper::toOrderLineResponse)
                .toList();
    }
}
