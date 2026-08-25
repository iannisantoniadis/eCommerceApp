package com.example.ecomerce;

import com.example.ecomerce.order.*;
import com.example.ecomerce.orderLine.OrderLineMapper;
import com.example.ecomerce.orderLine.OrderLineRepository;
import com.example.ecomerce.orderLine.OrderLineRequest;
import com.example.ecomerce.orderLine.OrderLineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrderLineService.class, OrderLineMapper.class, OrderMapper.class})
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "application.config.customer-url=http://localhost:9999",
        "application.config.product-url=http://localhost:9998",
        "application.config.payment-url=http://localhost:9997",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9997"
})
public class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderLineService orderLineService;

    @Autowired
    private TestEntityManager entityManager;

    private Order buildOrder(String reference) {
        return Order.builder()
                .reference(reference)
                .totalAmount(new BigDecimal("250.00"))
                .paymentMethod(PaymentMethodEnum.CREDIT_CARD)
                .status(OrderStatusEnum.PENDING)
                .customerId("customer-123")
                .build();
    }

    private OrderLineRequest buildOrderLineRequest(Long orderId, Long productId, Double quantity) {
        return new OrderLineRequest(
                null,
                new BigDecimal("125.00"),
                orderId,
                productId,
                quantity
        );
    }

    @Test
    @DisplayName("save - persists data and generates id")
    void save_PersistsOrder() {
        // GIVEN
        String ref = "REF-123";
        var order = buildOrder(ref);

        // WHEN
        var saved = orderRepository.save(order);

        // THEN
        assertNotNull(saved.getId());
        assertEquals(ref, saved.getReference());
        assertEquals(OrderStatusEnum.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedDate());
    }

    @Test
    @DisplayName("updateOrderStatus - updates status 'PENDING'")
    void updateOrderStatus_UpdatesStatus(){
        // GIVEN
        String ref = "REF-123";
        var order = entityManager.persistAndFlush(buildOrder(ref));

        // WHEN
        orderRepository.updateOrderStatus(order.getId(), OrderStatusEnum.CONFIRMED);
        entityManager.clear(); // to make sure that the cache is clean and the data is taken from DB

        // THEN
        var updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatusEnum.CONFIRMED, updated.getStatus());
    }

    @Test
    @DisplayName("updateOrderStatus - does nothing for unknown id")
    void updateOrderStatus_DoesNothing(){
        // WHEN and THEN - should not throw
        assertDoesNotThrow(() -> orderRepository.updateOrderStatus(999L, OrderStatusEnum.CONFIRMED));
    }

    @Test
    @DisplayName("findById - returns order for existing id")
    void findById_ReturnsOrder() {
        // GIVEN
        String ref = "REF-123";
        var order = entityManager.persistAndFlush(buildOrder(ref));

        //WHEN
        var result = orderRepository.findById(order.getId());

        //THEN
        assertTrue(result.isPresent());
        assertEquals(ref, result.get().getReference());

    }

    @Test
    @DisplayName("findById - returns empty for unknown id")
    void findById_ReturnsEmpty(){
        assertTrue(orderRepository.findById(999L).isEmpty());
    }

    @Test
    @DisplayName("findAll - returns correct page size and order")
    void findAll_ReturnsCorrectPaginatedResult() {
        // GIVEN
        for (int i = 1; i <= 3; i++) {
            entityManager.persistAndFlush(buildOrder("REF-00" + i));
        }

        // WHEN
        var page = orderRepository.findAll(PageRequest.of(0, 2, Sort.by("id")));

        // THEN
        assertEquals(2, page.getSize());
        assertEquals(3, page.getTotalElements());
        // check ordering by id
        assertTrue(page.getContent().get(0).getId() <
                page.getContent().get(1).getId());
    }

    @Test
    @DisplayName("saveOrderLine - persists all lines associated to order")
    void saveOrderLines_PersistsAllLines() {
        // GIVEN
        var order = entityManager.persistAndFlush(buildOrder("REF-123"));
        Long orderId = order.getId();

        var requests = List.of(
                buildOrderLineRequest(orderId, 1L, 2d),
                buildOrderLineRequest(orderId, 2L, 1d)
        );

        // WHEN
        var savedIds = orderLineService.saveOrderLines(requests);

        //THEN
        assertEquals(2, savedIds.size());

        var lines = orderLineRepository.findAllByOrderId(orderId);
        assertEquals(2, lines.size());
        assertTrue(lines.stream().anyMatch(line -> line.getProductId().equals(1L)));
        assertTrue(lines.stream().anyMatch(line -> line.getProductId().equals(2L)));
    }

    @Test
    @DisplayName("toOrderResponse - maps fields correctly")
    void toOrderResponse_MapsFieldsCorrectly() {
        // GIVEN
        String ref = "REF=123";
        var order = entityManager.persistAndFlush(buildOrder(ref));

        // WHEN
        var response = orderMapper.toOrderResponse(order);

        // THEN
        assertEquals(order.getId(), response.id());
        assertEquals(order.getReference(), response.reference());
        assertEquals(order.getStatus(), response.status());
        assertEquals("customer-123", response.customerId());

    }

    @Test
    @DisplayName("save - correctly increments version on update")
    void save_incrementVersionOnUpdate() {
        // GIVEN
        var order = entityManager.persistAndFlush(buildOrder("REF123"));
        assertEquals(0L, order.getVersion());

        // WHEN
        order.setStatus(OrderStatusEnum.COMPLETED);
        var updated = orderRepository.save(order);
        entityManager.flush();

        // THEN
        assertEquals(1L, updated.getVersion());
    }

    @Test
    @DisplayName("updateOrderStatus (bulk @Modifying query) - does NOT increment version, unlike save()")
    void updateOrderStatus_BulkUpdate_VersionBehavior() {
        var order = entityManager.persistAndFlush(buildOrder("REF123"));

        orderRepository.updateOrderStatus(order.getId(), OrderStatusEnum.COMPLETED);
        entityManager.clear(); // forces a fresh read from DB, bypassing the stale in-memory entity

        var reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatusEnum.COMPLETED, reloaded.getStatus());
        assertEquals(0L, reloaded.getVersion()); // confirms bulk update bypassed optimistic locking
    }
}
