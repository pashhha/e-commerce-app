package com.pavils.ecommerce.order;

import com.pavils.ecommerce.customer.CustomerClient;
import com.pavils.ecommerce.exception.BusinessException;
import com.pavils.ecommerce.kafka.OrderConfirmation;
import com.pavils.ecommerce.kafka.OrderProducer;
import com.pavils.ecommerce.orderline.OrderLineRequest;
import com.pavils.ecommerce.orderline.OrderLineService;
import com.pavils.ecommerce.product.ProductClient;
import com.pavils.ecommerce.product.PurchaseRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderMapper mapper;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;

    public Integer createOrder(@Valid OrderRequest request) {

        //check the customer --> OpenFeign
        var customer = customerClient.findCustomerById(request.customerId())
                .orElseThrow(() -> new BusinessException("Cannot create order:: Customer not found with the provided ID::" + request.customerId()));

        //purchase the product --> product_ms
        var purchasedProducts = productClient.purchaseRequest(request.products());

        //persist order
        var order = orderRepository.save(mapper.toOrder(request));

        //persist order lines
        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        //todo start payment process

        //send the order confirmation --> notifications_ms (Kafka)
        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        request.amount(),
                        request.paymentMethod(),
                        customer,
                        purchasedProducts
                )
        );

        return order.getId();
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll()
                .stream()
                .map(mapper::fromOrder)
                .toList();
    }

    public OrderResponse findById(Integer orderId) {
        return orderRepository.findById(orderId)
                .map(mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No order found with the provided ID: %d",  orderId))
                );
    }
}
