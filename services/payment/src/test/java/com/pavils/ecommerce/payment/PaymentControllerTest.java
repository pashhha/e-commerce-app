package com.pavils.ecommerce.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService service;

    // Required because @EnableJpaAuditing on PaymentApplication conflicts with @WebMvcTest
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPayment_ValidRequest_Returns200WithPaymentId() throws Exception {
        Customer customer = new Customer("cust-1", "John", "Doe", "john@example.com");
        PaymentRequest request = new PaymentRequest(
                null, new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD, 1, "REF-001", customer
        );
        when(service.createPayment(any(PaymentRequest.class))).thenReturn(42);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void createPayment_DelegatesRequestToService() throws Exception {
        Customer customer = new Customer("cust-2", "Jane", "Doe", "jane@example.com");
        PaymentRequest request = new PaymentRequest(
                null, new BigDecimal("50.00"), PaymentMethod.PAYPAL, 2, "REF-002", customer
        );
        when(service.createPayment(any(PaymentRequest.class))).thenReturn(7);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(service).createPayment(any(PaymentRequest.class));
    }
}
