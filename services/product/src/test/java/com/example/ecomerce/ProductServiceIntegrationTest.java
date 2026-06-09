package com.example.ecomerce;

import com.example.ecomerce.category.Category;
import com.example.ecomerce.exception.ProductPurchaseException;
import com.example.ecomerce.product.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("products_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private ProductMapper mapper;
    private ProductService service;

    @BeforeEach
    void setUp() {
        mapper = new ProductMapper();
        service = new ProductService(repository, mapper);
    }

    private Category persistCategory() {
        return entityManager.persistAndFlush(
                Category.builder()
                        .name("Electronics")
                        .description("Electronic products and stuff")
                        .build()
        );
    }

    private Product buildProduct(String name, Double quantity, Category category) {
        return Product.builder()
                .name(name)
                .description("Description")
                .availableQuantity(quantity)
                .price(new BigDecimal("100"))
                .category(category)
                .build();
    }

//    PURCHASE

    @Test
    @DisplayName("purchase products - deducts stock correctly for valid request")
    void purchaseProducts_DeductsStock() {
        //GIVEN
        String name = "Laptop";
        Double quantity = 10d;
        Double boughtQuantity = 3d;
        var category = persistCategory();
        var product = entityManager.persistAndFlush(buildProduct(name, quantity, category));

        var request  = new ProductPurchaseRequest(product.getId(), boughtQuantity);

        // WHEN
        var result = service.purchaseProducts(List.of(request));

        // THEN
        assertEquals(1, result.size());
        assertEquals(boughtQuantity, result.getFirst().quantity());

        var updated = repository.findById(product.getId()).orElseThrow();
        assertEquals(quantity - boughtQuantity, updated.getAvailableQuantity());
    }

    @Test
    @DisplayName("purchaseProducts - throws when requested quality exceeds stocks")
    void purchaseProducts_ThrowsOnInsufficientStock() {
        //GIVEN
        String name = "Laptop";
        Double quantity = 10d;
        Double boughtQuantity = 20d;
        var category = persistCategory();
        var product = entityManager.persistAndFlush(buildProduct(name, quantity, category));

        var request  = new ProductPurchaseRequest(product.getId(), boughtQuantity);

        //WHEN / THEN
        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(List.of(request)));

        //Check stock
        var unchangedStock = repository.findById(product.getId()).orElseThrow();
        assertEquals(quantity, unchangedStock.getAvailableQuantity());
    }

    @Test
    @DisplayName("purchaseProducts - throws when product id does not exist")
    void purchaseProducts_ThrowsOnMissingProduct() {
        var request = List.of(new ProductPurchaseRequest(999L, 1d));

        assertThrows(ProductPurchaseException.class, () -> service.purchaseProducts(request));
    }

    @Test
    @DisplayName("purchaseProducts - aggregates quantities for duplicate product ids")
    void purchaseProducts_AggregatesDuplicateIds() {
        // GIVEN
        String name = "Laptop";
        Double quantity = 10d;
        Double boughtQuantity1 = 1d;
        Double boughtQuantity2 = 2d;
        var category = persistCategory();
        var product = entityManager.persistAndFlush(buildProduct(name, quantity, category));

        var request = List.of(new ProductPurchaseRequest(product.getId(), boughtQuantity1),
                new ProductPurchaseRequest(product.getId(), boughtQuantity2));

        //WHEN
        var result = service.purchaseProducts(request);

        //THEN
        assertEquals(1, result.size());
        assertEquals(boughtQuantity1 + boughtQuantity2, result.getFirst().quantity());

        var updated = repository.findById(result.getFirst().productId()).orElseThrow();

        assertEquals(quantity - boughtQuantity2 - boughtQuantity1, updated.getAvailableQuantity());

    }

//    RESTORE

    @Test
    @DisplayName("restoreProducts - restores stock correctly")
    void restoreProducts_RestoresStock() {
        // GIVEN
        String name = "Laptop";
        Double quantity = 10d;
        Double boughtQuantity = 20d;
        var category = persistCategory();
        var product = entityManager.persistAndFlush(buildProduct(name, quantity, category));

        // WHEN
        service.restoreProducts(List.of(new ProductPurchaseRequest(product.getId(), boughtQuantity)));

        // THEN
        var updated = repository.findById(product.getId()).orElseThrow();
        assertEquals(quantity + boughtQuantity, updated.getAvailableQuantity());
    }

    @Test
    @DisplayName("findById - returns product response for existing id")
    void findById_ReturnsProduct() {
        String name = "Laptop";
        Double quantity = 10d;
        var category = persistCategory();
        var product = entityManager.persistAndFlush(buildProduct(name, quantity, category));

        var result = service.findById(product.getId());

        assertNotNull(result);
        assertEquals(product.getId(), result.id());
        assertEquals(product.getName(), result.name());
        assertEquals(product.getCategory().getName(), result.categoryName());
        assertEquals(product.getCategory().getDescription(), result.categoryDescription());
    }

    @Test
    @DisplayName("findById - throws EntityNotFoundException for unknown id")
    void findById_ThrowsExceptionForUnknownId(){
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> service.findById(999L));
    }
}
