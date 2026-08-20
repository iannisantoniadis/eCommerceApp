package com.example.ecomerce;

import com.example.ecomerce.payment.Payment;
import com.example.ecomerce.payment.PaymentMethodEnum;
import com.example.ecomerce.payment.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
// stuff known since compile time - they do not change, for instance these properties should be off at all times in testing context
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
})
// without this we run tests on in-memory H2 DB, because it's quicker, but we need postgres specific behavior
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PaymentRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments_test")
            .withUsername("test")
            .withPassword("test");

// these are computed at runtime , their values might not exist until something else is instantiated
    //e.g. postgres.getJdbcUrl() is not known since the testcontainer chooses a random port at each run to avoid port collisions
// , so one cannot guess the url at compile time
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
//        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private PaymentRepository repository;

    private Payment buildPayment(Long orderId) {
        return Payment.builder()
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethodEnum.CREDIT_CARD)
                .orderId(orderId)
                .build();
    }

    @Test
    @DisplayName("saveAndFlush - saves properly and returns object with id")
    void saveAndFlush_succeeds() {
        Payment payment = buildPayment(101L);

        Payment saved = repository.saveAndFlush(payment);

        assertNotNull(saved.getId());
        assertEquals(101L, saved.getOrderId());
    }

    @Test
    @DisplayName("saveAndFlush - duplicate id save throws Exception")
    void saveAndFlush_duplicateId() {
        var id = 200L;
        repository.saveAndFlush(buildPayment(id));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(buildPayment(id)));
    }

}
