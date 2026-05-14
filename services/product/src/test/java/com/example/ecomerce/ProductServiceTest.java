package com.example.ecomerce;

import com.example.ecomerce.exception.ProductPurchaseException;
import com.example.ecomerce.exception.ProductRestoreException;
import com.example.ecomerce.product.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock private ProductRepository repository;
    @Mock private ProductMapper mapper;

    @InjectMocks private ProductService productService;

    // Shared test data
    private static final Long PRODUCT_ID_1 = 1L;
    private static final Long PRODUCT_ID_2 = 2L;
    private static final Double AVAILABLE_QTY = 10d;
    private static final BigDecimal PRICE = new BigDecimal("100");

    private Product buildProduct(Long id, Double availableQuantity) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .description("Description " + id)
                .availableQuantity(availableQuantity)
                .price(PRICE)
                .build();
    }

    private ProductPurchaseRequest buildPurchaseRequest(Long productId, Double quantity) {
        return new ProductPurchaseRequest(productId, quantity);
    }

    private ProductPurchaseResponse buildPurchaseResponse(Long productId, Double quantity) {
        return new ProductPurchaseResponse(productId, "Product " + productId,
                "Description " + productId, PRICE, quantity);
    }

    // createProduct

    @Test
    @DisplayName("createProduct - success: returns saved product id")
    void createProduct_Success() {
        // GIVEN
        var request = new ProductRequest("Product", "Description",
                AVAILABLE_QTY, PRICE, 1L);
        var product = buildProduct(PRODUCT_ID_1, AVAILABLE_QTY);

        when(mapper.toProduct(request)).thenReturn(product);
        when(repository.save(product)).thenReturn(product);

        // WHEN
        Long id = productService.createProduct(request);

        // THEN
        assertNotNull(id);
        assertEquals(PRODUCT_ID_1, id);
        verify(repository, times(1)).save(product);
    }

    // purchaseProducts

    @Test
    @DisplayName("purchaseProducts - success: single product, sufficient stock")
    void purchaseProducts_SingleProduct_Success() {
        // GIVEN
        var product = buildProduct(PRODUCT_ID_1, AVAILABLE_QTY);
        var request = List.of(buildPurchaseRequest(PRODUCT_ID_1, 3d));
        var expectedResponse = buildPurchaseResponse(PRODUCT_ID_1, 3d);

        when(repository.findAllByIdInOrderById(List.of(PRODUCT_ID_1)))
                .thenReturn(List.of(product));
        when(mapper.toProductPurchaseResponse(any(), any()))
                .thenReturn(expectedResponse);

        // WHEN
        var result = productService.purchaseProducts(request);

        // THEN
        assertEquals(1, result.size());
        assertEquals(7d, product.getAvailableQuantity()); // 10 - 3 = 7
        verify(repository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("purchaseProducts - success: multiple distinct products")
    void purchaseProducts_MultipleProducts_Success() {
        // GIVEN
        var product1 = buildProduct(PRODUCT_ID_1, 10d);
        var product2 = buildProduct(PRODUCT_ID_2, 5d);
        var requests = List.of(
                buildPurchaseRequest(PRODUCT_ID_1, 2d),
                buildPurchaseRequest(PRODUCT_ID_2, 3d)
        );

        when(repository.findAllByIdInOrderById(any()))
                .thenReturn(List.of(product1, product2));
        when(mapper.toProductPurchaseResponse(any(), any()))
                .thenReturn(buildPurchaseResponse(PRODUCT_ID_1, 2d),
                        buildPurchaseResponse(PRODUCT_ID_2, 3d));

        // WHEN
        var result = productService.purchaseProducts(requests);

        // THEN
        assertEquals(2, result.size());
        assertEquals(8d, product1.getAvailableQuantity()); // 10 - 2
        assertEquals(2d, product2.getAvailableQuantity()); // 5 - 3
    }

    @Test
    @DisplayName("purchaseProducts - duplicate product ids: quantities are aggregated")
    void purchaseProducts_DuplicateIds_QuantitiesAggregated() {
        // GIVEN — same product twice, quantities should sum to 3
        var product = buildProduct(PRODUCT_ID_1, AVAILABLE_QTY);
        var requests = List.of(
                buildPurchaseRequest(PRODUCT_ID_1, 1d),
                buildPurchaseRequest(PRODUCT_ID_1, 2d)
        );

        when(repository.findAllByIdInOrderById(List.of(PRODUCT_ID_1)))
                .thenReturn(List.of(product));
        when(mapper.toProductPurchaseResponse(any(), any()))
                .thenReturn(buildPurchaseResponse(PRODUCT_ID_1, 3d));

        // WHEN
        var result = productService.purchaseProducts(requests);

        // THEN
        assertEquals(1, result.size());             // one response, not two
        assertEquals(7d, product.getAvailableQuantity()); // 10 - (1+2) = 7
    }

    @Test
    @DisplayName("purchaseProducts - throws ProductPurchaseException for missing product")
    void purchaseProducts_MissingProduct_ThrowsException() {
        // GIVEN
        var requests = List.of(buildPurchaseRequest(999L, 1d));
        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of());

        // WHEN & THEN
        var exception = assertThrows(ProductPurchaseException.class,
                () -> productService.purchaseProducts(requests));

        assertTrue(exception.getMessage().contains("999"));
        verifyNoMoreInteractions(repository); // saveAll never called
    }

    @Test
    @DisplayName("purchaseProducts - throws ProductPurchaseException for insufficient stock")
    void purchaseProducts_InsufficientStock_ThrowsException() {
        // GIVEN — requesting 15 but only 10 available
        var product = buildProduct(PRODUCT_ID_1, AVAILABLE_QTY);
        var requests = List.of(buildPurchaseRequest(PRODUCT_ID_1, 15d));

        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of(product));

        // WHEN & THEN
        var exception = assertThrows(ProductPurchaseException.class,
                () -> productService.purchaseProducts(requests));

        assertTrue(exception.getMessage().contains("insufficient quantity"));
        assertTrue(exception.getMessage().contains(String.valueOf(PRODUCT_ID_1)));

        // Stock should NOT be deducted
        assertEquals(AVAILABLE_QTY, product.getAvailableQuantity());
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("purchaseProducts - partial insufficient: all insufficient products reported together")
    void purchaseProducts_PartialInsufficient_AllReported() {
        // GIVEN — product1 ok, product2 insufficient
        var product1 = buildProduct(PRODUCT_ID_1, 10d);
        var product2 = buildProduct(PRODUCT_ID_2, 2d);
        var requests = List.of(
                buildPurchaseRequest(PRODUCT_ID_1, 1d),  // ok
                buildPurchaseRequest(PRODUCT_ID_2, 5d)   // insufficient
        );

        when(repository.findAllByIdInOrderById(any()))
                .thenReturn(List.of(product1, product2));

        // WHEN & THEN
        var exception = assertThrows(ProductPurchaseException.class,
                () -> productService.purchaseProducts(requests));

        assertTrue(exception.getMessage().contains(String.valueOf(PRODUCT_ID_2)));
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("purchaseProducts - stock is correctly deducted after purchase")
    void purchaseProducts_StockDeductedCorrectly() {
        // GIVEN
        var product = buildProduct(PRODUCT_ID_1, 10d);
        var requests = List.of(buildPurchaseRequest(PRODUCT_ID_1, 4d));

        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of(product));
        when(mapper.toProductPurchaseResponse(any(), any()))
                .thenReturn(buildPurchaseResponse(PRODUCT_ID_1, 4d));

        // WHEN
        productService.purchaseProducts(requests);

        // THEN
        assertEquals(6d, product.getAvailableQuantity()); // 10 - 4
        verify(repository, times(1)).saveAll(argThat(products -> {
            var list = new ArrayList<>((Collection) products);
            assertEquals(1, list.size());
            assertEquals(6d, ((Product) list.getFirst()).getAvailableQuantity());
            return true;
        }));
    }

    // restoreProducts
    @Test
    @DisplayName("restoreProducts - success: quantities restored correctly")
    void restoreProducts_Success() {
        // GIVEN
        var product = buildProduct(PRODUCT_ID_1, 7d); // currently 7 after a purchase
        var requests = List.of(buildPurchaseRequest(PRODUCT_ID_1, 3d));

        when(repository.findAllByIdInOrderById(List.of(PRODUCT_ID_1)))
                .thenReturn(List.of(product));

        // WHEN
        productService.restoreProducts(requests);

        // THEN
        assertEquals(10d, product.getAvailableQuantity()); // 7 + 3 restored
        verify(repository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("restoreProducts - duplicate ids: quantities aggregated before restore")
    void restoreProducts_DuplicateIds_Aggregated() {
        // GIVEN — restoring same product twice, total should be +5
        var product = buildProduct(PRODUCT_ID_1, 5d);
        var requests = List.of(
                buildPurchaseRequest(PRODUCT_ID_1, 2d),
                buildPurchaseRequest(PRODUCT_ID_1, 3d)
        );

        when(repository.findAllByIdInOrderById(List.of(PRODUCT_ID_1)))
                .thenReturn(List.of(product));

        // WHEN
        productService.restoreProducts(requests);

        // THEN
        assertEquals(10d, product.getAvailableQuantity()); // 5 + (2+3)
        verify(repository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("restoreProducts - throws ProductPurchaseException for invalid product ids")
    void restoreProducts_InvalidIds_ThrowsException() {
        // GIVEN
        var requests = List.of(buildPurchaseRequest(999L, 3d));
        when(repository.findAllByIdInOrderById(any())).thenReturn(List.of());

        // WHEN & THEN
        var exception = assertThrows(ProductRestoreException.class,
                () -> productService.restoreProducts(requests));

        assertTrue(exception.getMessage().contains("999"));
        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("restoreProducts - multiple products restored correctly")
    void restoreProducts_MultipleProducts_Success() {
        // GIVEN
        var product1 = buildProduct(PRODUCT_ID_1, 3d);
        var product2 = buildProduct(PRODUCT_ID_2, 1d);
        var requests = List.of(
                buildPurchaseRequest(PRODUCT_ID_1, 5d),
                buildPurchaseRequest(PRODUCT_ID_2, 4d)
        );

        when(repository.findAllByIdInOrderById(any()))
                .thenReturn(List.of(product1, product2));

        // WHEN
        productService.restoreProducts(requests);

        // THEN
        assertEquals(8d, product1.getAvailableQuantity());  // 3 + 5
        assertEquals(5d, product2.getAvailableQuantity());  // 1 + 4
        verify(repository, times(1)).saveAll(any());
    }
}
