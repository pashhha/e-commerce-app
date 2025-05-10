package com.pavils.ecommerce.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        Integer id,

        @NotNull(message = "Product name is required")
        String name,

        @NotNull(message = "Description is required")
        String description,

        @Positive(message = "Quantity should be positive")
        Double availableQuantity,

        @NotNull(message = "Price is required")
        BigDecimal price,

        @NotNull(message = "Product category is required")
        Integer categoryId
) {
}
