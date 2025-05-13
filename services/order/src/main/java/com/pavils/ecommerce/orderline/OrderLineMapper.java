package com.pavils.ecommerce.orderline;

import com.pavils.ecommerce.order.Order;

public class OrderLineMapper {
    public OrderLine toOrderLine(OrderLineRequest request) {
        return OrderLine
                .builder()
                .id(request.id())
                .order(
                        Order.builder()
                                .id(request.orderId())
                                .build()
                )
                .productId(request.productId())
                .quantity(request.quantity())
                .build();
    }
}
