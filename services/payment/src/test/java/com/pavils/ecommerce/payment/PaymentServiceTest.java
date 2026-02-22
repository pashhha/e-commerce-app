package com.pavils.ecommerce.payment;

import com.pavils.ecommerce.notification.NotificationProducer;
import com.pavils.ecommerce.notification.PaymentNotificationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentMapper mapper;

    @Mock
    private NotificationProducer producer;

    @InjectMocks
    private PaymentService service;

    @Captor
    private ArgumentCaptor<PaymentNotificationRequest> notificationCaptor;

    private final Customer customer =
            new Customer("cust-1", "John", "Doe", "john@example.com");

    @Test
    void createPayment_ValidRequest_SavesPaymentAndSendsNotification() {
        PaymentRequest request = new PaymentRequest(
                null, new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD, 1, "REF-001", customer
        );
        Payment savedPayment = Payment.builder().id(42).amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD).orderId(1).build();

        when(mapper.toPayment(request)).thenReturn(savedPayment);
        when(repository.save(savedPayment)).thenReturn(savedPayment);

        service.createPayment(request);

        verify(mapper).toPayment(request);
        verify(repository).save(savedPayment);
        verify(producer).sendNotification(any(PaymentNotificationRequest.class));
    }

    @Test
    void createPayment_ReturnsIdOfSavedPayment() {
        PaymentRequest request = new PaymentRequest(
                null, new BigDecimal("200.00"), PaymentMethod.PAYPAL, 2, "REF-002", customer
        );
        Payment savedPayment = Payment.builder().id(99).amount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.PAYPAL).orderId(2).build();

        when(mapper.toPayment(request)).thenReturn(savedPayment);
        when(repository.save(savedPayment)).thenReturn(savedPayment);

        Integer result = service.createPayment(request);

        assertThat(result).isEqualTo(99);
    }

    @Test
    void createPayment_NotificationContainsCorrectCustomerAndOrderFields() {
        PaymentRequest request = new PaymentRequest(
                null, new BigDecimal("75.50"), PaymentMethod.BITCOIN, 3, "REF-003", customer
        );
        Payment savedPayment = Payment.builder().id(7).amount(new BigDecimal("75.50"))
                .paymentMethod(PaymentMethod.BITCOIN).orderId(3).build();

        when(mapper.toPayment(request)).thenReturn(savedPayment);
        when(repository.save(savedPayment)).thenReturn(savedPayment);

        service.createPayment(request);

        verify(producer).sendNotification(notificationCaptor.capture());
        PaymentNotificationRequest notification = notificationCaptor.getValue();

        assertThat(notification.orderReference()).isEqualTo("REF-003");
        assertThat(notification.amount()).isEqualByComparingTo(new BigDecimal("75.50"));
        assertThat(notification.paymentMethod()).isEqualTo(PaymentMethod.BITCOIN);
        assertThat(notification.customerFirstname()).isEqualTo("John");
        assertThat(notification.customerLastname()).isEqualTo("Doe");
        assertThat(notification.customerEmail()).isEqualTo("john@example.com");
    }
}
