package com.example.ecomerce;

import com.example.ecomerce.product.Product;
import com.example.ecomerce.product.ProductRepository;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class ProductRepositoryIntegrationTest {

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

    private Product buildProduct(String name, Double quantity) {
        return Product.builder()
                .name(name)
                .description("Description")
                .availableQuantity(quantity)
                .price(new BigDecimal("100"))
                .build();
    }

    @Test
    @DisplayName("findAllByIdInOrderById - returns products ordered by id")
    void findAllByIdInOrderById_ReturnsOrderedResults() {
        // GIVEN
        var product1 = entityManager.persistAndFlush(buildProduct("Product A", 10d));
        var product2 = entityManager.persistAndFlush(buildProduct("Product B", 5d));

        // WHEN
        var result = repository.findAllByIdInOrderById(
                List.of(product2.getId(), product1.getId()) // intentionally reversed
        );

        // THEN
        assertEquals(2, result.size());
        assertTrue(result.get(0).getId() < result.get(1).getId()); // ordered by id
    }

    @Test
    @DisplayName("findAllByIdInOrderById - returns empty list for unknown ids")
    void findAllByIdInOrderById_UnknownIds_ReturnsEmpty() {
        // WHEN
        var result = repository.findAllByIdInOrderById(List.of(999L, 998L));

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("save - persists product and generates id")
    void save_PersistsProduct() {
        // GIVEN
        var product = buildProduct("New Product", 20d);

        // WHEN
        var saved = repository.save(product);

        // THEN
        assertNotNull(saved.getId());
        assertEquals("New Product", saved.getName());
        assertEquals(20d, saved.getAvailableQuantity());
    }

    @Test
    @DisplayName("save - version increments on update (optimistic locking)")
    void save_VersionIncrementsOnUpdate() {
        // GIVEN
        var product = entityManager.persistAndFlush(buildProduct("Product", 10d));
        assertEquals(0L, product.getVersion());

        // WHEN
        product.setAvailableQuantity(8d);
        var updated = repository.save(product);
        entityManager.flush();

        // THEN
        assertEquals(1L, updated.getVersion());
    }

}
