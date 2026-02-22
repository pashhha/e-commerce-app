package com.pavils.ecommerce.product;

import com.pavils.ecommerce.category.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toProduct_ValidRequest_MapsAllFields() {
        ProductRequest request = new ProductRequest(1, "Widget", "A widget", 10.0, BigDecimal.valueOf(9.99), 42);

        Product product = mapper.toProduct(request);

        assertThat(product.getId()).isEqualTo(1);
        assertThat(product.getName()).isEqualTo("Widget");
        assertThat(product.getDescription()).isEqualTo("A widget");
        assertThat(product.getAvailableQuantity()).isEqualTo(10.0);
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        assertThat(product.getCategory().getId()).isEqualTo(42);
    }

    @Test
    void toProduct_NullId_ProducesProductWithNullId() {
        ProductRequest request = new ProductRequest(null, "Widget", "A widget", 10.0, BigDecimal.valueOf(9.99), 42);

        Product product = mapper.toProduct(request);

        assertThat(product.getId()).isNull();
    }

    @Test
    void toProductResponse_ValidEntity_MapsAllFields() {
        Category category = Category.builder()
                .id(7)
                .name("Electronics")
                .description("Electronic items")
                .build();
        Product product = Product.builder()
                .id(1)
                .name("Widget")
                .description("A widget")
                .availableQuantity(10.0)
                .price(BigDecimal.valueOf(9.99))
                .category(category)
                .build();

        ProductResponse response = mapper.toProductResponse(product);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.description()).isEqualTo("A widget");
        assertThat(response.availableQuantity()).isEqualTo(10.0);
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        assertThat(response.categoryId()).isEqualTo(7);
        assertThat(response.categoryName()).isEqualTo("Electronics");
        assertThat(response.categoryDescription()).isEqualTo("Electronic items");
    }

    @Test
    void toProductPurchaseResponse_ValidEntity_MapsFieldsAndQuantity() {
        Category category = Category.builder().id(1).build();
        Product product = Product.builder()
                .id(1)
                .name("Widget")
                .description("A widget")
                .availableQuantity(5.0)
                .price(BigDecimal.valueOf(9.99))
                .category(category)
                .build();
        double purchasedQuantity = 3.0;

        ProductPurchaseResponse response = mapper.toProductPurchaseResponse(product, purchasedQuantity);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.description()).isEqualTo("A widget");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        assertThat(response.quantity()).isEqualTo(3.0);
    }
}
