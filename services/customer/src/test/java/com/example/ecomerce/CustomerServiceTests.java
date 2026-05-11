package com.example.ecomerce;

import com.example.ecomerce.customer.*;
import com.example.ecomerce.exception.CustomerBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.mongodb.assertions.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTests {

    @Mock private CustomerRepository repository;
    @Mock private CustomerMapper mapper;

    @InjectMocks
    private CustomerService customerService;

    // Shared test data
    private static final String CUSTOMER_ID = "60d5f1b7c8d5e42c18b4c3d2";
    private static final String EMAIL = "randomEmail@domain.com";
    private static final String FIRSTNAME = "FirstName";
    private static final String LASTNAME = "LastName";

    private CustomerRequest buildRequest() {
        return new CustomerRequest(
                CUSTOMER_ID,
                "FirstName",
                "LastName",
                EMAIL,
                null // so far useless for testing purposes
        );
    }

    private Customer buildCustomer() {
        var customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setFirstname(FIRSTNAME);
        customer.setLastname(LASTNAME);
        customer.setEmail(EMAIL);
        return customer;
    }

    private CustomerResponse buildCustomerResponse() {
        return new CustomerResponse(CUSTOMER_ID, FIRSTNAME, LASTNAME, EMAIL, null);
    }

    // createCustomer

    @Test
    @DisplayName("createCustomer - success: returns String customer id")
    void createCustomer_Success(){
        // GIVEN
        var request = buildRequest();
        var customer = buildCustomer();

        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(mapper.toCustomer(request)).thenReturn(customer);
        when(repository.save(customer)).thenReturn(customer);

        // WHEN
        String id = customerService.createCustomer(request);

        // THEN
        assertNotNull(id);
        assertEquals(CUSTOMER_ID, id);
        verify(repository, times(1)).save(customer);
    }

    @Test
    @DisplayName("createCustomer - throws CustomerBusinessException when email already in use")
    void createCustomer_EmailAlreadyInUse() {
        // GIVEN
        var request = buildRequest();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(buildCustomer()));

        // WHEN & THEN
        var exception = assertThrows(CustomerBusinessException.class,
                () -> customerService.createCustomer(request));

        assertTrue(exception.getMessage().contains(EMAIL));

        // Save should never be called
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    // updateCustomer

    @Test
    @DisplayName("updateCustomer - success: merges and saves customer")
    void updateCustomer_Success() {
        // GIVEN
        var request = buildRequest();
        var existing = buildCustomer();

        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // WHEN
        customerService.updateCustomer(request);

        // THEN
        verify(repository, times(1)).save(existing);
        assertEquals(FIRSTNAME, existing.getFirstname());
        assertEquals(LASTNAME, existing.getLastname());
        assertEquals(EMAIL, existing.getEmail());
    }

    @Test
    @DisplayName("updateCustomer - throws CustomerBusinessException when customer not found")
    void updateCustomer_CustomerNotFound() {
        // GIVEN
        var request = buildRequest();
        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        // WHEN & THEN
        var exception = assertThrows(CustomerBusinessException.class,
                () -> customerService.updateCustomer(request));

        assertTrue(exception.getMessage().contains(CUSTOMER_ID));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateCustomer - partial update: only non-empty fields are merged")
    void updateCustomer_PartialUpdate() {
        // GIVEN — only firstname is updated, rest stays as is
        var partialRequest = new CustomerRequest(
                CUSTOMER_ID,
                "NewFirstName",  // only this changes
                "",              // empty, should be ignored
                "",              // empty, should be ignored
                null             // null, should be ignored
        );
        var existing = buildCustomer();
        String originalLastname = existing.getLastname();
        String originalEmail = existing.getEmail();

        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // WHEN
        customerService.updateCustomer(partialRequest);

        // THEN — only firstname changes
        assertEquals("NewFirstName", existing.getFirstname());
        assertEquals(originalLastname, existing.getLastname()); // unchanged
        assertEquals(originalEmail, existing.getEmail());       // unchanged
        assertNull(existing.getAddress());                      // unchanged
    }

    // findAllCustomers

    @Test
    @DisplayName("findAllCustomers - returns all mapped customers")
    void findAllCustomers_ReturnsMappedList() {
        // GIVEN
        int page = 0;
        int size = 10;
        var customer = buildCustomer();
        var response = buildCustomerResponse();
        Page<Customer> customerPage = new PageImpl<>(List.of(customer));

        when(repository.findAll(any(Pageable.class))).thenReturn(customerPage);
        when(mapper.toCustomerResponse(customer)).thenReturn(response);

        // WHEN
        var results = customerService.findAllCustomers(page, size);

        // THEN
        assertEquals(1, results.getSize());
        assertEquals(CUSTOMER_ID, results.getContent().getFirst().id());
    }

    @Test
    @DisplayName("findAllCustomers - returns empty list when no customers exist")
    void findAllCustomers_EmptyList() {
        // GIVEN
        int page = 0;
        int size = 10;
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        // WHEN
        var results = customerService.findAllCustomers(page, size);

        // THEN
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(mapper);
    }
}
