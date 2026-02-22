package com.pavils.ecommerce.handlers;

import com.pavils.ecommerce.exception.CustomerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleCustomerNotFoundException_Returns404WithMessage() {
        CustomerNotFoundException ex = new CustomerNotFoundException("Customer not found");

        ResponseEntity<String> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Customer not found");
    }

    @Test
    void handleCustomerNotFoundException_EmptyMessage_Returns404WithEmptyBody() {
        CustomerNotFoundException ex = new CustomerNotFoundException("");

        ResponseEntity<String> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void handleMethodArgumentNotValidException_SingleError_ReturnsBadRequestWithErrorMap() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "customerRequest");
        bindingResult.addError(new FieldError("customerRequest", "firstname", "Customer firstname is required"));

        Method method = Object.class.getMethod("toString");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsKey("firstname");
        assertThat(response.getBody().errors().get("firstname")).isEqualTo("Customer firstname is required");
    }

    @Test
    void handleMethodArgumentNotValidException_MultipleErrors_ReturnsAllErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "customerRequest");
        bindingResult.addError(new FieldError("customerRequest", "firstname", "Customer firstname is required"));
        bindingResult.addError(new FieldError("customerRequest", "email", "Is not a valid email address"));

        Method method = Object.class.getMethod("toString");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors()).containsKeys("firstname", "email");
    }
}
