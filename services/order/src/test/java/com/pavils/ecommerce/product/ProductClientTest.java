package com.pavils.ecommerce.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductClientTest {

    private static final String PRODUCT_URL = "http://localhost:8080/api/v1/products";

    private ProductClient productClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        productClient = new ProductClient(restTemplate);
        ReflectionTestUtils.setField(productClient, "productUrl", PRODUCT_URL);
    }

    @Test
    void purchaseRequest_SuccessfulResponse_ReturnsParsedList() throws Exception {
        List<PurchaseResponse> expected = List.of(
                new PurchaseResponse(1, "Widget", "A widget", new BigDecimal("9.99"), 2.0)
        );
        String responseBody = objectMapper.writeValueAsString(expected);

        mockServer.expect(requestTo(PRODUCT_URL + "/purchase"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<PurchaseResponse> result = productClient.purchaseRequest(
                List.of(new PurchaseRequest(1, 2.0))
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(1);
        assertThat(result.get(0).name()).isEqualTo("Widget");
        mockServer.verify();
    }

    @Test
    void purchaseRequest_ContentTypeHeaderIsSet() {
        mockServer.expect(requestTo(PRODUCT_URL + "/purchase"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        productClient.purchaseRequest(List.of(new PurchaseRequest(1, 1.0)));

        mockServer.verify();
    }

    @Test
    void purchaseRequest_4xxResponse_ThrowsHttpClientErrorException() {
        // KNOWN DEAD CODE: The isError() check in ProductClient.purchaseRequest() is unreachable.
        // RestTemplate.exchange() throws HttpClientErrorException on 4xx before the isError()
        // branch can execute, making the BusinessException throw in that branch dead code.
        mockServer.expect(requestTo(PRODUCT_URL + "/purchase"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> productClient.purchaseRequest(List.of(new PurchaseRequest(1, 1.0))))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void purchaseRequest_5xxResponse_ThrowsHttpServerErrorException() {
        // Same as 4xx: RestTemplate throws HttpServerErrorException on 5xx before isError() check.
        mockServer.expect(requestTo(PRODUCT_URL + "/purchase"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> productClient.purchaseRequest(List.of(new PurchaseRequest(1, 1.0))))
                .isInstanceOf(HttpServerErrorException.class);
    }
}
