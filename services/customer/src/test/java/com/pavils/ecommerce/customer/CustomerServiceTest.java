package com.pavils.ecommerce.customer;

import com.pavils.ecommerce.exception.CustomerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    @InjectMocks
    private CustomerService service;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    private final Address address = new Address("Main St", "10", "12345");

    // ── createCustomer ──────────────────────────────────────────────────────

    @Test
    void createCustomer_ValidRequest_SavesAndReturnsId() {
        CustomerRequest request = new CustomerRequest(
                null, "John", "Doe", "john@example.com", address
        );
        Customer saved = Customer.builder().id("cust-1").firstname("John").build();

        when(mapper.toCustomer(request)).thenReturn(saved);
        when(repository.save(saved)).thenReturn(saved);

        String result = service.createCustomer(request);

        assertThat(result).isEqualTo("cust-1");
        verify(mapper).toCustomer(request);
        verify(repository).save(saved);
    }

    // ── updateCustomer ──────────────────────────────────────────────────────

    @Test
    void updateCustomer_CustomerNotFound_ThrowsCustomerNotFoundException() {
        CustomerRequest request = new CustomerRequest(
                "missing-id", "John", "Doe", "john@example.com", address
        );
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomer(request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasFieldOrPropertyWithValue("msg",
                        "Cannot update customer:: No customer found with the provided id:: missing-id");

        verify(repository, never()).save(any());
    }

    @Test
    void updateCustomer_NonBlankFirstname_UpdatesFirstname() {
        Customer existing = Customer.builder()
                .id("cust-1").firstname("OldFirst").lastname("OldLast").email("old@example.com").build();
        CustomerRequest request = new CustomerRequest("cust-1", "NewFirst", null, null, null);

        when(repository.findById("cust-1")).thenReturn(Optional.of(existing));

        service.updateCustomer(request);

        verify(repository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getFirstname()).isEqualTo("NewFirst");
    }

    @Test
    void updateCustomer_NonBlankLastname_SetsFirstnameDueToBug() {
        // KNOWN BUG: mergeCustomer calls customer.setFirstname(request.lastname()) instead of
        // customer.setLastname(request.lastname()). Lastname update overwrites firstname.
        Customer existing = Customer.builder()
                .id("cust-1").firstname("OldFirst").lastname("OldLast").email("old@example.com").build();
        CustomerRequest request = new CustomerRequest("cust-1", null, "NewLast", null, null);

        when(repository.findById("cust-1")).thenReturn(Optional.of(existing));

        service.updateCustomer(request);

        verify(repository).save(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        // Bug: setFirstname is called with the lastname value
        assertThat(saved.getFirstname()).isEqualTo("NewLast");
        // lastname is unchanged due to the bug
        assertThat(saved.getLastname()).isEqualTo("OldLast");
    }

    @Test
    void updateCustomer_NonBlankEmail_SetsFirstnameDueToBug() {
        // KNOWN BUG: mergeCustomer calls customer.setFirstname(request.email()) instead of
        // customer.setEmail(request.email()). Email update overwrites firstname.
        Customer existing = Customer.builder()
                .id("cust-1").firstname("OldFirst").lastname("OldLast").email("old@example.com").build();
        CustomerRequest request = new CustomerRequest("cust-1", null, null, "new@example.com", null);

        when(repository.findById("cust-1")).thenReturn(Optional.of(existing));

        service.updateCustomer(request);

        verify(repository).save(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        // Bug: setFirstname is called with the email value
        assertThat(saved.getFirstname()).isEqualTo("new@example.com");
        // email is unchanged due to the bug
        assertThat(saved.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    void updateCustomer_NonNullAddress_UpdatesAddress() {
        Customer existing = Customer.builder()
                .id("cust-1").firstname("John").lastname("Doe").email("john@example.com").build();
        Address newAddress = new Address("New St", "99", "00000");
        CustomerRequest request = new CustomerRequest("cust-1", null, null, null, newAddress);

        when(repository.findById("cust-1")).thenReturn(Optional.of(existing));

        service.updateCustomer(request);

        verify(repository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getAddress()).isEqualTo(newAddress);
    }

    @Test
    void updateCustomer_BlankFields_NothingUpdated() {
        Customer existing = Customer.builder()
                .id("cust-1").firstname("John").lastname("Doe").email("john@example.com").build();
        CustomerRequest request = new CustomerRequest("cust-1", "", "", "", null);

        when(repository.findById("cust-1")).thenReturn(Optional.of(existing));

        service.updateCustomer(request);

        verify(repository).save(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        assertThat(saved.getFirstname()).isEqualTo("John");
        assertThat(saved.getLastname()).isEqualTo("Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
    }

    // ── findAllCustomers ────────────────────────────────────────────────────

    @Test
    void findAllCustomers_WithCustomers_ReturnsMappedList() {
        Customer c1 = Customer.builder().id("c1").build();
        Customer c2 = Customer.builder().id("c2").build();
        CustomerResponse r1 = new CustomerResponse("c1", "A", "B", "a@x.com", null);
        CustomerResponse r2 = new CustomerResponse("c2", "C", "D", "c@x.com", null);

        when(repository.findAll()).thenReturn(List.of(c1, c2));
        when(mapper.toCustomerResponse(c1)).thenReturn(r1);
        when(mapper.toCustomerResponse(c2)).thenReturn(r2);

        List<CustomerResponse> result = service.findAllCustomers();

        assertThat(result).hasSize(2).containsExactly(r1, r2);
    }

    @Test
    void findAllCustomers_EmptyRepository_ReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAllCustomers()).isEmpty();
        verify(mapper, never()).toCustomerResponse(any());
    }

    // ── existsById ──────────────────────────────────────────────────────────

    @Test
    void existsById_ExistingCustomer_ReturnsTrue() {
        Customer customer = Customer.builder().id("cust-1").build();
        when(repository.findById("cust-1")).thenReturn(Optional.of(customer));

        assertThat(service.existsById("cust-1")).isTrue();
    }

    @Test
    void existsById_NonExistentCustomer_ReturnsFalse() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThat(service.existsById("missing")).isFalse();
    }

    // ── findCustomerById ────────────────────────────────────────────────────

    @Test
    void findCustomerById_ExistingCustomer_ReturnsMappedResponse() {
        Customer customer = Customer.builder().id("cust-1").firstname("John").build();
        CustomerResponse response = new CustomerResponse("cust-1", "John", "Doe", "j@x.com", null);

        when(repository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(mapper.toCustomerResponse(customer)).thenReturn(response);

        assertThat(service.findCustomerById("cust-1")).isEqualTo(response);
    }

    @Test
    void findCustomerById_NonExistent_ThrowsCustomerNotFoundException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCustomerById("missing"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasFieldOrPropertyWithValue("msg",
                        "No customer found with the provided id:: missing");
    }

    // ── deleteCustomer ──────────────────────────────────────────────────────

    @Test
    void deleteCustomer_CallsRepositoryDeleteById() {
        service.deleteCustomer("cust-1");

        verify(repository).deleteById("cust-1");
    }
}
