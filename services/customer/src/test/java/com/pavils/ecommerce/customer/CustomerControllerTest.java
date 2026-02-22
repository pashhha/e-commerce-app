package com.pavils.ecommerce.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavils.ecommerce.exception.CustomerNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService service;

    @Autowired
    private ObjectMapper objectMapper;

    private final Address address = new Address("Main St", "1", "12345");

    // ── POST /api/v1/customers ──────────────────────────────────────────────

    @Test
    void createCustomer_ValidRequest_Returns200WithId() throws Exception {
        CustomerRequest request = new CustomerRequest(
                null, "John", "Doe", "john@example.com", address
        );
        when(service.createCustomer(any())).thenReturn("cust-abc");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("cust-abc"));
    }

    @Test
    void createCustomer_NullFirstname_Returns400WithValidationError() throws Exception {
        String body = """
                {"firstname":null,"lastname":"Doe","email":"john@example.com",
                 "address":{"street":"Main St","houseNumber":"1","zipCode":"12345"}}
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstname").value("Customer firstname is required"));
    }

    @Test
    void createCustomer_InvalidEmail_Returns400WithValidationError() throws Exception {
        String body = """
                {"firstname":"John","lastname":"Doe","email":"not-an-email",
                 "address":{"street":"Main St","houseNumber":"1","zipCode":"12345"}}
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createCustomer_NullAddress_Returns400WithValidationError() throws Exception {
        String body = """
                {"firstname":"John","lastname":"Doe","email":"john@example.com","address":null}
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.address").value("Customer address is required"));
    }

    // ── PUT /api/v1/customers ───────────────────────────────────────────────

    @Test
    void updateCustomer_ValidRequest_Returns202() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "cust-1", "John", "Doe", "john@example.com", address
        );
        doNothing().when(service).updateCustomer(any());

        mockMvc.perform(put("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void updateCustomer_CustomerNotFound_Returns404() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "missing", "John", "Doe", "john@example.com", address
        );
        doThrow(new CustomerNotFoundException("Cannot update customer:: No customer found with the provided id:: missing"))
                .when(service).updateCustomer(any());

        mockMvc.perform(put("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/customers ───────────────────────────────────────────────

    @Test
    void findAllCustomers_Returns200WithList() throws Exception {
        List<CustomerResponse> customers = List.of(
                new CustomerResponse("c1", "Alice", "Smith", "alice@x.com", null),
                new CustomerResponse("c2", "Bob", "Jones", "bob@x.com", null)
        );
        when(service.findAllCustomers()).thenReturn(customers);

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("c1"))
                .andExpect(jsonPath("$[0].firstname").value("Alice"))
                .andExpect(jsonPath("$[1].id").value("c2"));
    }

    @Test
    void findAllCustomers_EmptyList_Returns200WithEmptyArray() throws Exception {
        when(service.findAllCustomers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/v1/customers/exists/{id} ──────────────────────────────────

    @Test
    void existsById_ExistingCustomer_Returns200WithTrue() throws Exception {
        when(service.existsById("cust-1")).thenReturn(true);

        mockMvc.perform(get("/api/v1/customers/exists/cust-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void existsById_NonExistentCustomer_Returns200WithFalse() throws Exception {
        when(service.existsById("missing")).thenReturn(false);

        mockMvc.perform(get("/api/v1/customers/exists/missing"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ── GET /api/v1/customers/{id} ──────────────────────────────────────────

    @Test
    void findCustomerById_ExistingCustomer_Returns200() throws Exception {
        CustomerResponse response = new CustomerResponse("c1", "Alice", "Smith", "alice@x.com", address);
        when(service.findCustomerById("c1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.firstname").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@x.com"));
    }

    @Test
    void findCustomerById_NotFound_Returns404() throws Exception {
        when(service.findCustomerById("missing"))
                .thenThrow(new CustomerNotFoundException("No customer found with the provided id:: missing"));

        mockMvc.perform(get("/api/v1/customers/missing"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/customers/{id} ──────────────────────────────────────

    @Test
    void deleteCustomer_ValidId_Returns202() throws Exception {
        doNothing().when(service).deleteCustomer("cust-1");

        mockMvc.perform(delete("/api/v1/customers/cust-1"))
                .andExpect(status().isAccepted());

        verify(service).deleteCustomer("cust-1");
    }
}
