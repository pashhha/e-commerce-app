package com.pavils.ecommerce.orderline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderLineController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "application.config.customer-url=http://localhost:8080",
        "application.config.payment-url=http://localhost:8081",
        "application.config.product-url=http://localhost:8082"
})
class OrderLineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderLineService service;

    // Required because @EnableJpaAuditing on OrderApplication conflicts with @WebMvcTest
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void findByOrderId_WithLines_Returns200WithLineList() throws Exception {
        when(service.findByOrderId(1)).thenReturn(List.of(
                new OrderLineResponse(1, 2.0),
                new OrderLineResponse(2, 3.5)
        ));

        mockMvc.perform(get("/api/v1/order-lines/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].quantity").value(2.0))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].quantity").value(3.5));
    }

    @Test
    void findByOrderId_NoLines_Returns200WithEmptyList() throws Exception {
        when(service.findByOrderId(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/order-lines/order/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findByOrderId_PathVariable_CorrectIdPassedToService() throws Exception {
        when(service.findByOrderId(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/order-lines/order/5"))
                .andExpect(status().isOk());

        verify(service).findByOrderId(5);
    }
}
