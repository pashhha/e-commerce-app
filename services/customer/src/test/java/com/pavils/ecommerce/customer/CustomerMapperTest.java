package com.pavils.ecommerce.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
    }

    @Test
    void toCustomer_ValidRequest_MapsAllFields() {
        Address address = new Address("Main St", "10", "12345");
        CustomerRequest request = new CustomerRequest(
                "cust-1", "John", "Doe", "john@example.com", address
        );

        Customer customer = mapper.toCustomer(request);

        assertThat(customer.getId()).isEqualTo("cust-1");
        assertThat(customer.getFirstname()).isEqualTo("John");
        assertThat(customer.getLastname()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo("john@example.com");
        assertThat(customer.getAddress()).isEqualTo(address);
    }

    @Test
    void toCustomer_NullRequest_ReturnsNull() {
        assertThat(mapper.toCustomer(null)).isNull();
    }

    @Test
    void toCustomer_NullId_ProducesCustomerWithNullId() {
        Address address = new Address("Oak Ave", "2", "99999");
        CustomerRequest request = new CustomerRequest(
                null, "Jane", "Smith", "jane@example.com", address
        );

        Customer customer = mapper.toCustomer(request);

        assertThat(customer.getId()).isNull();
        assertThat(customer.getFirstname()).isEqualTo("Jane");
    }

    @Test
    void toCustomerResponse_ValidEntity_MapsAllFields() {
        Address address = new Address("Elm St", "5", "54321");
        Customer customer = Customer.builder()
                .id("cust-2")
                .firstname("Alice")
                .lastname("Brown")
                .email("alice@example.com")
                .address(address)
                .build();

        CustomerResponse response = mapper.toCustomerResponse(customer);

        assertThat(response.id()).isEqualTo("cust-2");
        assertThat(response.firstname()).isEqualTo("Alice");
        assertThat(response.lastname()).isEqualTo("Brown");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.address()).isEqualTo(address);
    }

    @Test
    void toCustomerResponse_NullAddress_MapsNullAddress() {
        Customer customer = Customer.builder()
                .id("cust-3")
                .firstname("Bob")
                .lastname("Jones")
                .email("bob@example.com")
                .address(null)
                .build();

        CustomerResponse response = mapper.toCustomerResponse(customer);

        assertThat(response.address()).isNull();
    }
}
