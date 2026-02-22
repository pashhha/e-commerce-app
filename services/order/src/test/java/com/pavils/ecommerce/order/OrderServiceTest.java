package com.pavils.ecommerce.order;

import com.pavils.ecommerce.customer.CustomerClient;
import com.pavils.ecommerce.customer.CustomerResponse;
import com.pavils.ecommerce.exception.BusinessException;
import com.pavils.ecommerce.kafka.OrderConfirmation;
import com.pavils.ecommerce.kafka.OrderProducer;
import com.pavils.ecommerce.orderline.OrderLineRequest;
import com.pavils.ecommerce.orderline.OrderLineService;
import com.pavils.ecommerce.payment.PaymentClient;
import com.pavils.ecommerce.product.ProductClient;
import com.pavils.ecommerce.product.PurchaseRequest;
import com.pavils.ecommerce.product.PurchaseResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderMapper mapper;

    @Mock
    private OrderLineService orderLineService;

    @Mock
    private OrderProducer orderProducer;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<OrderLineRequest> orderLineCaptor;

    private final CustomerResponse customer =
            new CustomerResponse("customer-123", "John", "Doe", "john@example.com");

    @Test
    void createOrder_HappyPath_ReturnsOrderId() {
        OrderRequest request = new OrderRequest(
                null, "REF-001", new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD,
                "customer-123", List.of(new PurchaseRequest(1, 2.0))
        );
        Order savedOrder = Order.builder()
                .id(42).reference("REF-001")
                .totalAmount(new BigDecimal("100.00")).paymentMethod(PaymentMethod.CREDIT_CARD)
                .customerId("customer-123").build();

        when(customerClient.findCustomerById("customer-123")).thenReturn(Optional.of(customer));
        when(productClient.purchaseRequest(any())).thenReturn(
                List.of(new PurchaseResponse(1, "Widget", "A widget", new BigDecimal("50.00"), 2.0))
        );
        when(mapper.toOrder(request)).thenReturn(savedOrder);
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        Integer result = orderService.createOrder(request);

        assertThat(result).isEqualTo(42);
        verify(orderLineService).saveOrderLine(any(OrderLineRequest.class));
        verify(paymentClient).requestOrderPayment(any());
        verify(orderProducer).sendOrderConfirmation(any(OrderConfirmation.class));
    }

    @Test
    void createOrder_CustomerNotFound_ThrowsBusinessException() {
        OrderRequest request = new OrderRequest(
                null, "REF-002", new BigDecimal("50.00"), PaymentMethod.PAYPAL,
                "unknown-customer", List.of(new PurchaseRequest(1, 1.0))
        );
        when(customerClient.findCustomerById("unknown-customer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("msg",
                        "Cannot create order:: Customer not found with the provided ID::unknown-customer");

        verify(orderRepository, never()).save(any());
        verify(paymentClient, never()).requestOrderPayment(any());
        verify(orderProducer, never()).sendOrderConfirmation(any());
    }

    @Test
    void createOrder_MultipleProducts_SavesAllOrderLines() {
        List<PurchaseRequest> products = List.of(
                new PurchaseRequest(1, 1.0),
                new PurchaseRequest(2, 2.0),
                new PurchaseRequest(3, 3.0)
        );
        OrderRequest request = new OrderRequest(
                null, "REF-003", new BigDecimal("300.00"), PaymentMethod.VISA_CARD,
                "customer-123", products
        );
        Order savedOrder = Order.builder()
                .id(10).reference("REF-003")
                .totalAmount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.VISA_CARD)
                .customerId("customer-123").build();

        when(customerClient.findCustomerById("customer-123")).thenReturn(Optional.of(customer));
        when(productClient.purchaseRequest(any())).thenReturn(List.of());
        when(mapper.toOrder(request)).thenReturn(savedOrder);
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        orderService.createOrder(request);

        verify(orderLineService, times(3)).saveOrderLine(orderLineCaptor.capture());
        List<OrderLineRequest> capturedRequests = orderLineCaptor.getAllValues();
        assertThat(capturedRequests).allMatch(r -> r.orderId().equals(10));
        assertThat(capturedRequests.get(0).productId()).isEqualTo(1);
        assertThat(capturedRequests.get(1).productId()).isEqualTo(2);
        assertThat(capturedRequests.get(2).productId()).isEqualTo(3);
    }

    @Test
    void findAll_WithOrders_ReturnsMappedList() {
        Order order1 = Order.builder().id(1).build();
        Order order2 = Order.builder().id(2).build();
        OrderResponse response1 = new OrderResponse(1, "REF-1", BigDecimal.TEN, PaymentMethod.PAYPAL, "cust-1");
        OrderResponse response2 = new OrderResponse(2, "REF-2", BigDecimal.ONE, PaymentMethod.CREDIT_CARD, "cust-2");

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));
        when(mapper.fromOrder(order1)).thenReturn(response1);
        when(mapper.fromOrder(order2)).thenReturn(response2);

        List<OrderResponse> result = orderService.findAll();

        assertThat(result).hasSize(2).containsExactly(response1, response2);
    }

    @Test
    void findAll_EmptyRepository_ReturnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderResponse> result = orderService.findAll();

        assertThat(result).isEmpty();
        verify(mapper, never()).fromOrder(any());
    }

    @Test
    void findById_ExistingOrder_ReturnsMappedResponse() {
        Order order = Order.builder().id(7).reference("REF-7").build();
        OrderResponse response = new OrderResponse(7, "REF-7", BigDecimal.TEN, PaymentMethod.BITCOIN, "cust-7");

        when(orderRepository.findById(7)).thenReturn(Optional.of(order));
        when(mapper.fromOrder(order)).thenReturn(response);

        OrderResponse result = orderService.findById(7);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_NonExistentOrder_ThrowsEntityNotFoundException() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
