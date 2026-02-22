package com.pavils.ecommerce.handlers;

import com.pavils.ecommerce.exception.EntityNotFoundException;
import com.pavils.ecommerce.exception.ProductPurchaseException;
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

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleProductPurchaseException_Returns400WithNullBody() {
        // Documents Bug #2: ProductPurchaseException does not call super(msg),
        // so getMessage() returns null and the response body is null.
        ProductPurchaseException ex = new ProductPurchaseException("some message");

        ResponseEntity<String> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void handleEntityNotFoundException_Returns400WithNullBody() {
        // Documents Bug #1: EntityNotFoundException does not call super(msg),
        // so getMessage() returns null and the response body is null.
        EntityNotFoundException ex = new EntityNotFoundException("some message");

        ResponseEntity<String> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void handleMethodArgumentNotValidException_SingleError_ReturnsBadRequestWithErrorMap() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class.getDeclaredMethod("handle", MethodArgumentNotValidException.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "name", "Product name is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsKey("name");
        assertThat(response.getBody().errors().get("name")).isEqualTo("Product name is required");
    }

    @Test
    void handleMethodArgumentNotValidException_MultipleErrors_ReturnsAllErrors() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class.getDeclaredMethod("handle", MethodArgumentNotValidException.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "name", "Product name is required"));
        bindingResult.addError(new FieldError("productRequest", "price", "Price is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handle(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
    }
}
