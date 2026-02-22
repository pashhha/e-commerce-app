package com.pavils.ecommerce.handlers;

import com.pavils.ecommerce.exception.BusinessException;
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
    void handleBusinessException_Returns404WithMessage() {
        BusinessException ex = new BusinessException("Order not found");

        ResponseEntity<String> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Order not found");
    }

    @Test
    void handleBusinessException_EmptyMessage_Returns404WithEmptyBody() {
        BusinessException ex = new BusinessException("");

        ResponseEntity<String> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void handleMethodArgumentNotValidException_SingleError_ReturnsBadRequestWithErrorMap() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "orderRequest");
        bindingResult.addError(new FieldError("orderRequest", "customerId", "must not be blank"));

        Method method = Object.class.getMethod("toString");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsKey("customerId");
        assertThat(response.getBody().errors().get("customerId")).isEqualTo("must not be blank");
    }

    @Test
    void handleMethodArgumentNotValidException_MultipleErrors_ReturnsAllErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "orderRequest");
        bindingResult.addError(new FieldError("orderRequest", "customerId", "must not be blank"));
        bindingResult.addError(new FieldError("orderRequest", "amount", "must be positive"));

        Method method = Object.class.getMethod("toString");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors()).containsKeys("customerId", "amount");
    }
}
