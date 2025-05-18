package com.pavils.ecommerce.orderline;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderLineService {

    final private OrderLineRepository repository;
    final private OrderLineMapper mapper;

    public void saveOrderLine(OrderLineRequest orderLineRequest) {
        repository.save(mapper.toOrderLine(orderLineRequest));
    }

    public List<OrderLineResponse> findByOrderId(Integer orderId) {
        return repository.findByOrderId(orderId)
                .stream()
                .map(mapper::toOrderLineResponse)
                .toList();
    }
}
