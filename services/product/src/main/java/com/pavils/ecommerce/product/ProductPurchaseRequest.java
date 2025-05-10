package com.pavils.ecommerce.product;

import jakarta.validation.constraints.NotNull;

public record ProductPurchaseRequest(
        @NotNull(message = "productId is mandatory")
        Integer productId,

        @NotNull(message = "quantity is mandatory")
        double quantity
) {
}
