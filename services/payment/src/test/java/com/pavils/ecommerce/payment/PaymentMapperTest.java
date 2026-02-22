package com.pavils.ecommerce.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private PaymentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentMapper();
    }

    @Test
    void toPayment_ValidRequest_MapsAllFields() {
        Customer customer = new Customer("cust-1", "John", "Doe", "john@example.com");
        PaymentRequest request = new PaymentRequest(
                5, new BigDecimal("150.00"), PaymentMethod.CREDIT_CARD, 10, "REF-001", customer
        );

        Payment payment = mapper.toPayment(request);

        assertThat(payment.getId()).isEqualTo(5);
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(payment.getOrderId()).isEqualTo(10);
    }

    @Test
    void toPayment_NullId_ProducesPaymentWithNullId() {
        Customer customer = new Customer("cust-2", "Jane", "Doe", "jane@example.com");
        PaymentRequest request = new PaymentRequest(
                null, new BigDecimal("50.00"), PaymentMethod.PAYPAL, 7, "REF-002", customer
        );

        Payment payment = mapper.toPayment(request);

        assertThat(payment.getId()).isNull();
    }

    @Test
    void toPayment_DoesNotMapCustomerOrReference() {
        // Mapper intentionally excludes customer and orderReference — only payment data is persisted
        Customer customer = new Customer("cust-3", "Alice", "Smith", "alice@example.com");
        PaymentRequest request = new PaymentRequest(
                1, new BigDecimal("75.00"), PaymentMethod.BITCOIN, 3, "REF-003", customer
        );

        Payment payment = mapper.toPayment(request);

        // No customer or reference fields on the Payment entity
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.BITCOIN);
        assertThat(payment.getOrderId()).isEqualTo(3);
    }
}
