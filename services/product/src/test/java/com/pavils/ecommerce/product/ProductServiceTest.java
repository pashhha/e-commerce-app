package com.pavils.ecommerce.product;

import com.pavils.ecommerce.category.Category;
import com.pavils.ecommerce.exception.EntityNotFoundException;
import com.pavils.ecommerce.exception.ProductPurchaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private ProductService service;

    private Product buildProduct(int id, double quantity, BigDecimal price) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .description("Desc " + id)
                .availableQuantity(quantity)
                .price(price)
                .category(Category.builder().id(1).build())
                .build();
    }

    @Test
    void createProduct_ValidRequest_SavesAndReturnsId() {
        ProductRequest request = new ProductRequest(null, "Widget", "A widget", 10.0, BigDecimal.valueOf(9.99), 1);
        Product product = buildProduct(5, 10.0, BigDecimal.valueOf(9.99));
        when(mapper.toProduct(request)).thenReturn(product);
        when(repository.save(product)).thenReturn(product);

        Integer id = service.createProduct(request);

        assertThat(id).isEqualTo(5);
        verify(mapper).toProduct(request);
        verify(repository).save(product);
    }

    @Test
    void purchaseProducts_AllProductsExist_SufficientStock_ReturnsResponses() {
        Product p1 = buildProduct(1, 10.0, BigDecimal.valueOf(5.0));
        Product p2 = buildProduct(2, 20.0, BigDecimal.valueOf(10.0));
        List<ProductPurchaseRequest> requests = List.of(
                new ProductPurchaseRequest(1, 3.0),
                new ProductPurchaseRequest(2, 5.0)
        );
        when(repository.findAllByIdInOrderById(List.of(1, 2))).thenReturn(List.of(p1, p2));
        when(mapper.toProductPurchaseResponse(p1, 3.0))
                .thenReturn(new ProductPurchaseResponse(1, "Product 1", "Desc 1", BigDecimal.valueOf(5.0), 3.0));
        when(mapper.toProductPurchaseResponse(p2, 5.0))
                .thenReturn(new ProductPurchaseResponse(2, "Product 2", "Desc 2", BigDecimal.valueOf(10.0), 5.0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ProductPurchaseResponse> result = service.purchaseProducts(requests);

        assertThat(result).hasSize(2);
        assertThat(p1.getAvailableQuantity()).isEqualTo(7.0);
        assertThat(p2.getAvailableQuantity()).isEqualTo(15.0);
        verify(repository).save(p1);
        verify(repository).save(p2);
    }

    @Test
    void purchaseProducts_ProductIdsMismatch_ThrowsProductPurchaseException() {
        Product p1 = buildProduct(1, 10.0, BigDecimal.valueOf(5.0));
        List<ProductPurchaseRequest> requests = List.of(
                new ProductPurchaseRequest(1, 3.0),
                new ProductPurchaseRequest(2, 2.0)
        );
        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of(p1));

        assertThatThrownBy(() -> service.purchaseProducts(requests))
                .isInstanceOf(ProductPurchaseException.class);
    }

    @Test
    void purchaseProducts_InsufficientStock_ThrowsProductPurchaseException() {
        Product p1 = buildProduct(1, 2.0, BigDecimal.valueOf(5.0));
        List<ProductPurchaseRequest> requests = List.of(
                new ProductPurchaseRequest(1, 5.0)
        );
        when(repository.findAllByIdInOrderById(List.of(1))).thenReturn(List.of(p1));

        assertThatThrownBy(() -> service.purchaseProducts(requests))
                .isInstanceOf(ProductPurchaseException.class);
    }

    @Test
    void purchaseProducts_SortsRequestsByProductId_BeforeMatching() {
        // Requests arrive in order [id=3, id=1]; stored products ordered by id: [id=1, id=3]
        Product p1 = buildProduct(1, 10.0, BigDecimal.valueOf(5.0));
        Product p3 = buildProduct(3, 20.0, BigDecimal.valueOf(8.0));
        List<ProductPurchaseRequest> requests = List.of(
                new ProductPurchaseRequest(3, 4.0),
                new ProductPurchaseRequest(1, 3.0)
        );
        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of(p1, p3));
        when(mapper.toProductPurchaseResponse(any(), anyDouble()))
                .thenReturn(new ProductPurchaseResponse(1, "P1", "D1", BigDecimal.ONE, 3.0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.purchaseProducts(requests);

        // After sorting by productId, p1 matched with quantity 3.0 and p3 with 4.0
        assertThat(p1.getAvailableQuantity()).isEqualTo(7.0);
        assertThat(p3.getAvailableQuantity()).isEqualTo(16.0);
    }

    @Test
    void purchaseProducts_SavesEachProductAfterDeduction() {
        Product p1 = buildProduct(1, 10.0, BigDecimal.valueOf(5.0));
        Product p2 = buildProduct(2, 20.0, BigDecimal.valueOf(10.0));
        List<ProductPurchaseRequest> requests = List.of(
                new ProductPurchaseRequest(1, 2.0),
                new ProductPurchaseRequest(2, 3.0)
        );
        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of(p1, p2));
        when(mapper.toProductPurchaseResponse(any(), anyDouble()))
                .thenReturn(new ProductPurchaseResponse(1, "P1", "D1", BigDecimal.ONE, 2.0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.purchaseProducts(requests);

        verify(repository, times(2)).save(any(Product.class));
    }

    @Test
    void findById_ExistingProduct_ReturnsMappedResponse() {
        Product product = buildProduct(1, 10.0, BigDecimal.valueOf(9.99));
        ProductResponse response = new ProductResponse(1, "Product 1", "Desc 1", 10.0, BigDecimal.valueOf(9.99), 1, "Cat", "CatDesc");
        when(repository.findById(1)).thenReturn(Optional.of(product));
        when(mapper.toProductResponse(product)).thenReturn(response);

        ProductResponse result = service.findById(1);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_NonExistent_ThrowsEntityNotFoundException() {
        // Bug #1: EntityNotFoundException does not call super(msg), so getMessage() returns null
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_WithProducts_ReturnsMappedList() {
        Product p1 = buildProduct(1, 10.0, BigDecimal.valueOf(5.0));
        Product p2 = buildProduct(2, 20.0, BigDecimal.valueOf(10.0));
        ProductResponse r1 = new ProductResponse(1, "Product 1", "Desc 1", 10.0, BigDecimal.valueOf(5.0), 1, "Cat", "CatDesc");
        ProductResponse r2 = new ProductResponse(2, "Product 2", "Desc 2", 20.0, BigDecimal.valueOf(10.0), 1, "Cat", "CatDesc");
        when(repository.findAll()).thenReturn(List.of(p1, p2));
        when(mapper.toProductResponse(p1)).thenReturn(r1);
        when(mapper.toProductResponse(p2)).thenReturn(r2);

        List<ProductResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(r1, r2);
    }

    @Test
    void findAll_Empty_ReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<ProductResponse> result = service.findAll();

        assertThat(result).isEmpty();
    }
}
