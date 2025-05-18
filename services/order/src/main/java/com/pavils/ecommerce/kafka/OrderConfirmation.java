package com.pavils.ecommerce.kafka;

import com.pavils.ecommerce.customer.CustomerResponse;
import com.pavils.ecommerce.order.PaymentMethod;
import com.pavils.ecommerce.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {}
