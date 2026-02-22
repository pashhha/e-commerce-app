package com.pavils.ecommerce.order;

import com.pavils.ecommerce.product.PurchaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private OrderMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
    }

    @Test
    void toOrder_ValidRequest_MapsAllFields() {
        OrderRequest request = new OrderRequest(
                1, "REF-001", new BigDecimal("99.99"), PaymentMethod.CREDIT_CARD,
                "customer-123", List.of(new PurchaseRequest(1, 2.0))
        );

        Order order = mapper.toOrder(request);

        assertThat(order.getId()).isEqualTo(1);
        assertThat(order.getCustomerId()).isEqualTo("customer-123");
        assertThat(order.getReference()).isEqualTo("REF-001");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    void toOrder_NullId_ProducesOrderWithNullId() {
        OrderRequest request = new OrderRequest(
                null, "REF-002", new BigDecimal("10.00"), PaymentMethod.PAYPAL,
                "customer-456", List.of(new PurchaseRequest(2, 1.0))
        );

        Order order = mapper.toOrder(request);

        assertThat(order.getId()).isNull();
    }

    @Test
    void fromOrder_ValidEntity_MapsAllFields() {
        Order order = Order.builder()
                .id(5)
                .reference("REF-005")
                .totalAmount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.BITCOIN)
                .customerId("customer-789")
                .build();

        OrderResponse response = mapper.fromOrder(order);

        assertThat(response.id()).isEqualTo(5);
        assertThat(response.reference()).isEqualTo("REF-005");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.BITCOIN);
        assertThat(response.customerId()).isEqualTo("customer-789");
    }

    @Test
    void fromOrder_NullFields_MapsToNullFields() {
        Order order = Order.builder()
                .id(null)
                .reference(null)
                .totalAmount(null)
                .paymentMethod(null)
                .customerId(null)
                .build();

        OrderResponse response = mapper.fromOrder(order);

        assertThat(response.id()).isNull();
        assertThat(response.reference()).isNull();
        assertThat(response.amount()).isNull();
        assertThat(response.paymentMethod()).isNull();
        assertThat(response.customerId()).isNull();
    }
}
