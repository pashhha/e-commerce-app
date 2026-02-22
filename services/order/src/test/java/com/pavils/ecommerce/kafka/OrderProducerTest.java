package com.pavils.ecommerce.kafka;

import com.pavils.ecommerce.customer.CustomerResponse;
import com.pavils.ecommerce.order.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProducerTest {

    @Mock
    private KafkaTemplate<String, OrderConfirmation> kafkaTemplate;

    @InjectMocks
    private OrderProducer orderProducer;

    @Captor
    private ArgumentCaptor<Message<OrderConfirmation>> messageCaptor;

    private OrderConfirmation sampleConfirmation() {
        CustomerResponse customer = new CustomerResponse("cust-1", "Jane", "Doe", "jane@example.com");
        return new OrderConfirmation(
                "REF-001",
                new BigDecimal("99.99"),
                PaymentMethod.CREDIT_CARD,
                customer,
                List.of()
        );
    }

    @Test
    void sendOrderConfirmation_KafkaTemplateSendCalled() {
        OrderConfirmation confirmation = sampleConfirmation();

        orderProducer.sendOrderConfirmation(confirmation);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<OrderConfirmation> captured = messageCaptor.getValue();
        assertThat(captured.getPayload()).isEqualTo(confirmation);
    }

    @Test
    void sendOrderConfirmation_CorrectTopicHeader() {
        OrderConfirmation confirmation = sampleConfirmation();

        orderProducer.sendOrderConfirmation(confirmation);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<OrderConfirmation> captured = messageCaptor.getValue();
        assertThat(captured.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("order-topic");
    }

    @Test
    void sendOrderConfirmation_CalledExactlyOnce() {
        OrderConfirmation confirmation = sampleConfirmation();

        orderProducer.sendOrderConfirmation(confirmation);

        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }
}
