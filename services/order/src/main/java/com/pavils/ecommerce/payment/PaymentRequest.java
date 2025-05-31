package com.pavils.ecommerce.payment;

import com.pavils.ecommerce.customer.CustomerResponse;
import com.pavils.ecommerce.order.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer orderId,
        String orderReference,
        CustomerResponse customer
) {
}
