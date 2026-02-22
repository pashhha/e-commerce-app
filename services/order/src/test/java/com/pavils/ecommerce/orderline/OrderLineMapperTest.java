package com.pavils.ecommerce.orderline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLineMapperTest {

    private OrderLineMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderLineMapper();
    }

    @Test
    void toOrderLine_ValidRequest_MapsAllFields() {
        OrderLineRequest request = new OrderLineRequest(10, 5, 42, 3.0);

        OrderLine orderLine = mapper.toOrderLine(request);

        assertThat(orderLine.getId()).isEqualTo(10);
        assertThat(orderLine.getProductId()).isEqualTo(42);
        assertThat(orderLine.getQuantity()).isEqualTo(3.0);
        assertThat(orderLine.getOrder()).isNotNull();
        assertThat(orderLine.getOrder().getId()).isEqualTo(5);
    }

    @Test
    void toOrderLine_NullId_ProducesEntityWithNullId() {
        OrderLineRequest request = new OrderLineRequest(null, 5, 42, 1.0);

        OrderLine orderLine = mapper.toOrderLine(request);

        assertThat(orderLine.getId()).isNull();
    }

    @Test
    void toOrderLineResponse_ValidEntity_MapsIdAndQuantity() {
        OrderLine orderLine = OrderLine.builder()
                .id(7)
                .quantity(4.5)
                .build();

        OrderLineResponse response = mapper.toOrderLineResponse(orderLine);

        assertThat(response.id()).isEqualTo(7);
        assertThat(response.quantity()).isEqualTo(4.5);
    }
}
