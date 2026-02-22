package com.pavils.ecommerce.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavils.ecommerce.exception.BusinessException;
import com.pavils.ecommerce.product.PurchaseRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "application.config.customer-url=http://localhost:8080",
        "application.config.payment-url=http://localhost:8081",
        "application.config.product-url=http://localhost:8082"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // Required because @EnableJpaAuditing on OrderApplication conflicts with @WebMvcTest
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_ValidRequest_Returns200WithOrderId() throws Exception {
        OrderRequest request = new OrderRequest(
                null, "REF-001", new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD,
                "customer-123", List.of(new PurchaseRequest(1, 2.0))
        );
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(42);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void createOrder_BlankCustomerId_Returns400WithValidationError() throws Exception {
        // customerId="" triggers @NotBlank — GlobalExceptionHandler returns 400 with field errors
        String requestJson = """
                {
                    "reference": "REF-001",
                    "amount": 100.00,
                    "paymentMethod": "CREDIT_CARD",
                    "customerId": "",
                    "products": [{"productId": 1, "quantity": 2}]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerId").exists());
    }

    @Test
    void createOrder_EmptyProducts_Returns400() throws Exception {
        // products=[] triggers @NotEmpty
        String requestJson = """
                {
                    "reference": "REF-001",
                    "amount": 100.00,
                    "paymentMethod": "CREDIT_CARD",
                    "customerId": "customer-123",
                    "products": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_NegativeAmount_Returns400() throws Exception {
        // amount=-50 triggers @Positive
        String requestJson = """
                {
                    "reference": "REF-001",
                    "amount": -50,
                    "paymentMethod": "CREDIT_CARD",
                    "customerId": "customer-123",
                    "products": [{"productId": 1, "quantity": 2}]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_CustomerNotFound_Returns404() throws Exception {
        OrderRequest request = new OrderRequest(
                null, "REF-001", new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD,
                "unknown-customer", List.of(new PurchaseRequest(1, 2.0))
        );
        when(orderService.createOrder(any(OrderRequest.class)))
                .thenThrow(new BusinessException(
                        "Cannot create order:: Customer not found with the provided ID::unknown-customer"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_Returns200WithList() throws Exception {
        List<OrderResponse> orders = List.of(
                new OrderResponse(1, "REF-1", new BigDecimal("50.00"), PaymentMethod.PAYPAL, "cust-1"),
                new OrderResponse(2, "REF-2", new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD, "cust-2")
        );
        when(orderService.findAll()).thenReturn(orders);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reference").value("REF-1"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void findAll_EmptyList_Returns200WithEmptyArray() throws Exception {
        when(orderService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findById_ExistingOrder_Returns200() throws Exception {
        OrderResponse response = new OrderResponse(1, "REF-1", new BigDecimal("50.00"), PaymentMethod.PAYPAL, "cust-1");
        when(orderService.findById(1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reference").value("REF-1"))
                .andExpect(jsonPath("$.paymentMethod").value("PAYPAL"));
    }

    @Test
    void findById_NotFound_Returns500() {
        // KNOWN GAP: GlobalExceptionHandler does not handle EntityNotFoundException.
        // The service throws EntityNotFoundException but no @ExceptionHandler is registered for it.
        // In production this produces a 500 Internal Server Error.
        // In Spring 6.x / Spring Boot 3.x, MockMvc rethrows the unhandled exception
        // from perform() rather than translating it to a 500 response.
        when(orderService.findById(99)).thenThrow(
                new EntityNotFoundException("No order found with the provided ID: 99"));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/orders/99")))
                .rootCause()
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
