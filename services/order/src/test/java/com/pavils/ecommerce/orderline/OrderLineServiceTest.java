package com.pavils.ecommerce.orderline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderLineServiceTest {

    @Mock
    private OrderLineRepository repository;

    @Mock
    private OrderLineMapper mapper;

    @InjectMocks
    private OrderLineService service;

    @Test
    void saveOrderLine_ValidRequest_DelegatesToMapperThenRepository() {
        OrderLineRequest request = new OrderLineRequest(null, 1, 10, 2.0);
        OrderLine orderLine = OrderLine.builder().productId(10).quantity(2.0).build();
        when(mapper.toOrderLine(request)).thenReturn(orderLine);

        service.saveOrderLine(request);

        verify(mapper).toOrderLine(request);
        verify(repository).save(orderLine);
    }

    @Test
    void saveOrderLine_ExistingId_PassesIdToMapper() {
        OrderLineRequest request = new OrderLineRequest(5, 1, 10, 2.0);
        OrderLine orderLine = OrderLine.builder().id(5).productId(10).quantity(2.0).build();
        when(mapper.toOrderLine(argThat(r ->
                r.id().equals(5) &&
                r.orderId().equals(1) &&
                r.productId().equals(10) &&
                r.quantity() == 2.0
        ))).thenReturn(orderLine);

        service.saveOrderLine(request);

        verify(repository).save(orderLine);
    }

    @Test
    void findByOrderId_WithLines_ReturnsMappedResponses() {
        OrderLine line1 = OrderLine.builder().id(1).quantity(1.0).build();
        OrderLine line2 = OrderLine.builder().id(2).quantity(2.0).build();
        OrderLineResponse response1 = new OrderLineResponse(1, 1.0);
        OrderLineResponse response2 = new OrderLineResponse(2, 2.0);

        when(repository.findByOrderId(10)).thenReturn(List.of(line1, line2));
        when(mapper.toOrderLineResponse(line1)).thenReturn(response1);
        when(mapper.toOrderLineResponse(line2)).thenReturn(response2);

        List<OrderLineResponse> result = service.findByOrderId(10);

        assertThat(result).hasSize(2).containsExactly(response1, response2);
        verify(mapper, times(2)).toOrderLineResponse(any(OrderLine.class));
    }

    @Test
    void findByOrderId_NoLines_ReturnsEmptyList() {
        when(repository.findByOrderId(99)).thenReturn(List.of());

        List<OrderLineResponse> result = service.findByOrderId(99);

        assertThat(result).isEmpty();
        verify(mapper, never()).toOrderLineResponse(any());
    }
}
