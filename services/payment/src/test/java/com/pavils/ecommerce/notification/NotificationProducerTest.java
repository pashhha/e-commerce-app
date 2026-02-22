package com.pavils.ecommerce.notification;

import com.pavils.ecommerce.payment.PaymentMethod;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentNotificationRequest> kafkaTemplate;

    @InjectMocks
    private NotificationProducer producer;

    @Captor
    private ArgumentCaptor<Message<PaymentNotificationRequest>> messageCaptor;

    private PaymentNotificationRequest sampleRequest() {
        return new PaymentNotificationRequest(
                "REF-001",
                new BigDecimal("99.99"),
                PaymentMethod.CREDIT_CARD,
                "John",
                "Doe",
                "john@example.com"
        );
    }

    @Test
    void sendNotification_KafkaTemplateSendCalled() {
        PaymentNotificationRequest request = sampleRequest();

        producer.sendNotification(request);

        verify(kafkaTemplate).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).isEqualTo(request);
    }

    @Test
    void sendNotification_CorrectTopicHeader() {
        producer.sendNotification(sampleRequest());

        verify(kafkaTemplate).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getHeaders().get(KafkaHeaders.TOPIC))
                .isEqualTo("payment-topic");
    }

    @Test
    void sendNotification_CalledExactlyOnce() {
        producer.sendNotification(sampleRequest());

        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }
}
