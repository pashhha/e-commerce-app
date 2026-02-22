package com.pavils.ecommerce.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavils.ecommerce.exception.EntityNotFoundException;
import com.pavils.ecommerce.exception.ProductPurchaseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService service;

    @Test
    void createProduct_ValidRequest_Returns200WithId() throws Exception {
        ProductRequest request = new ProductRequest(null, "Widget", "A widget", 10.0, BigDecimal.valueOf(9.99), 1);
        when(service.createProduct(any())).thenReturn(5);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void createProduct_NullName_Returns400WithValidationError() throws Exception {
        ProductRequest request = new ProductRequest(null, null, "A widget", 10.0, BigDecimal.valueOf(9.99), 1);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createProduct_NegativeQuantity_Returns400() throws Exception {
        ProductRequest request = new ProductRequest(null, "Widget", "A widget", -1.0, BigDecimal.valueOf(9.99), 1);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void purchaseProducts_ValidRequest_Returns200WithList() throws Exception {
        List<ProductPurchaseRequest> requests = List.of(new ProductPurchaseRequest(1, 2.0));
        List<ProductPurchaseResponse> responses = List.of(
                new ProductPurchaseResponse(1, "Widget", "A widget", BigDecimal.valueOf(9.99), 2.0)
        );
        when(service.purchaseProducts(any())).thenReturn(responses);

        mockMvc.perform(post("/api/v1/products/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        // Documents Bug #3: controller calls service.purchaseProducts(request) twice;
        // the first result is silently discarded and only the second is returned.
        verify(service, times(2)).purchaseProducts(any());
    }

    @Test
    void purchaseProducts_ServiceThrows_Returns400() throws Exception {
        List<ProductPurchaseRequest> requests = List.of(new ProductPurchaseRequest(1, 2.0));
        when(service.purchaseProducts(any())).thenThrow(new ProductPurchaseException("Insufficient stock"));

        mockMvc.perform(post("/api/v1/products/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_ExistingProduct_Returns200() throws Exception {
        ProductResponse response = new ProductResponse(
                1, "Widget", "A widget", 10.0, BigDecimal.valueOf(9.99), 1, "Electronics", "Electronic items");
        when(service.findById(1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99));
    }

    @Test
    void findById_NotFound_Returns400() throws Exception {
        // Handler maps EntityNotFoundException to 400, not 404
        when(service.findById(99)).thenThrow(new EntityNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_Returns200WithList() throws Exception {
        List<ProductResponse> responses = List.of(
                new ProductResponse(1, "Widget A", "Desc A", 10.0, BigDecimal.valueOf(5.00), 1, "Cat", "CatDesc"),
                new ProductResponse(2, "Widget B", "Desc B", 20.0, BigDecimal.valueOf(10.00), 1, "Cat", "CatDesc")
        );
        when(service.findAll()).thenReturn(responses);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Widget A"))
                .andExpect(jsonPath("$[1].name").value("Widget B"));
    }

    @Test
    void findAll_EmptyList_Returns200WithEmptyArray() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
