package com.example.ecomerce;

import com.example.ecomerce.customer.Address;
import com.example.ecomerce.customer.Customer;
import com.example.ecomerce.customer.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
public class CustomerRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // No need for username and password for testing scenarios
        registry.add("spring.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.mongodb.port", mongoDBContainer::getFirstMappedPort);
        registry.add("spring.mongodb.database", () -> "customer_test");
    }

    @Autowired
    private CustomerRepository repository;

    // No transaction rollback for NoSql DBs so this is required for a clean slate
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private Customer buildCustomer(String firstname, String lastname, String email) {
        return Customer.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .address(Address.builder()
                        .street("street")
                        .zipCode("1234")
                        .build())
                .build();
    }

    @Test
    @DisplayName("save - persists customer and generates id")
    void save_PersistCustomer(){
        //GIVEN
        String firstName = "John";
        String lastName = "Doe";
        String email = "test@email.com";
        var customer = buildCustomer(firstName, lastName, email);

        //WHEN
        var saved = repository.save(customer);

        //THEN
        assertNotNull(saved.getId());
        assertEquals(firstName, saved.getFirstname());
        assertEquals(lastName, saved.getLastname());
        assertEquals(email, saved.getEmail());
    }

    @Test
    @DisplayName("save - version increments on update (optimistic locking)")
    void save_VersionIncrementsOnUpdate() {
        // GIVEN
        String firstName = "Johnny";
        String lastName = "Doe";
        String email = "test@email.com";
        var customer = repository.save(buildCustomer(firstName, lastName, email));
        assertEquals(0L, customer.getVersion());

        // WHEN
        customer.setFirstname(firstName);
        var updated = repository.save(customer);

        // THEN
        assertEquals(1L, updated.getVersion());
    }

    @Test
    @DisplayName("findByEmail - returns customer when email exists")
    void findByEmail_ReturnsCustomer_WhenEmailExists() {
        // GIVEN
        String firstName = "John";
        String lastName = "Doe";
        String email = "test@email.com";
        repository.save(buildCustomer(firstName, lastName, email));

        // WHEN
        var result = repository.findByEmail(email);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(firstName, result.get().getFirstname());
    }

    @Test
    @DisplayName("findByEmail - returns empty when email not found")
    void findByEmail_ReturnsEmpty_WhenEmailNotFound() {
        // WHEN
        var result = repository.findByEmail("whatever@example.com");

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findById - returns empty after deletion")
    void findById_ReturnsEmpty_AfterDeletion() {
        // GIVEN
        String firstName = "John";
        String lastName = "Doe";
        String email = "test@email.com";
        var saved = repository.save(buildCustomer(firstName, lastName, email));

        // WHEN
        repository.deleteById(saved.getId());

        // THEN
        assertTrue(repository.findById(saved.getId()).isEmpty());
    }

}
