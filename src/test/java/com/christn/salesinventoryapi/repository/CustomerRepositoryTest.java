package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer createCustomer(String name, String email, boolean deleted) {
        Customer customer = new Customer();
        customer.setFullName(name);
        customer.setEmail(email);
        customer.setDeleted(deleted);
        return customerRepository.save(customer);
    }

    @Test
    @DisplayName("findAllByDeletedFalse should return only active customers")
    void findAllByDeletedFalse_shouldReturnOnlyActiveCustomers() {
        // Given
        createCustomer("John Doe", "john@test.com", false);
        createCustomer("Jane Doe", "jane@test.com", true);
        createCustomer("Bob Smith", "bob@test.com", false);

        // When
        List<Customer> result = customerRepository.findAllByDeletedFalse();

        // Then
        assertThat(result)
                .hasSize(2)
                .allMatch(customer -> !customer.getDeleted());
    }

    @Test
    @DisplayName("existsByEmailAndDeletedFalse should return true only for active customers")
    void existsByEmailAndDeletedFalse_shouldWorkCorrectly() {
        // Given
        createCustomer("John Doe", "john@test.com", false);
        createCustomer("Jane Doe", "jane@test.com", true);

        // Then
        assertThat(customerRepository.existsByEmailAndDeletedFalse("john@test.com")).isTrue();
        assertThat(customerRepository.existsByEmailAndDeletedFalse("jane@test.com")).isFalse();
        assertThat(customerRepository.existsByEmailAndDeletedFalse("nope@test.com")).isFalse();
    }

    @Nested
    @DisplayName("findByIdAndDeletedFalse")
    class FindByIdAndDeletedFalseTests {

        @Test
        @DisplayName("Should return customer when active")
        void findByIdAndDeletedFalse_shouldReturnCustomer_whenActive() {
            // Given
            Customer customer = createCustomer("John Doe", "john@test.com", false);

            // When
            Optional<Customer> result = customerRepository.findByIdAndDeletedFalse(customer.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("john@test.com");
        }

        @Test
        @DisplayName("Should return empty when customer is deleted")
        void findByIdAndDeletedFalse_shouldReturnEmpty_whenDeleted() {
            // Given
            Customer customer = createCustomer("John Doe", "john@test.com", true);

            // When
            Optional<Customer> result = customerRepository.findByIdAndDeletedFalse(customer.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }
}
