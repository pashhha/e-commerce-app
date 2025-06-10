package com.pavils.ecommerce.product;


import com.pavils.ecommerce.exception.EntityNotFoundException;
import com.pavils.ecommerce.exception.ProductPurchaseException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public Integer createProduct(@Valid ProductRequest request) {
        var product = mapper.toProduct(request);
        return productRepository.save(product).getId();
    }

    public List<ProductPurchaseResponse> purchaseProducts(List<ProductPurchaseRequest> request) {
        var productIds = request
                .stream()
                .map(ProductPurchaseRequest::productId)
                .toList();

        var storedProducts = productRepository.findAllByIdInOrderById(productIds);

        if (productIds.size() != storedProducts.size()) {
            throw new ProductPurchaseException("One or more products does not exists");
        }

        var requestedProducts = request
                .stream()
                .sorted(Comparator.comparing(ProductPurchaseRequest::productId))
                .toList();

        var purchasedProducts = new ArrayList<ProductPurchaseResponse>();

        for (int i = 0; i < storedProducts.size(); i++) {
            var storedProduct = storedProducts.get(i);
            var requestedProduct = requestedProducts.get(i);

            if (storedProduct.getAvailableQuantity() < requestedProduct.quantity()) {
                throw new ProductPurchaseException("Insufficient stock quantity for product with id:: " + requestedProduct.productId());
            }

            var availableQuantityNew = storedProduct.getAvailableQuantity() - requestedProduct.quantity();
            storedProduct.setAvailableQuantity(availableQuantityNew);
            productRepository.save(storedProduct);

            purchasedProducts.add(mapper.toProductPurchaseResponse(storedProduct, requestedProduct.quantity()));
        }

        return purchasedProducts;
    }

    public ProductResponse findById(Integer id) {
        return mapper.toProductResponse(productRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID:: " + id)));
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(mapper::toProductResponse)
                .toList();
    }
}
