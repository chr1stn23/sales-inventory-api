package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Tests")
@ActiveProfiles("test")
public class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    //Helper method
    private Customer createCustomer(Long id, String fullName, String email) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName(fullName);
        customer.setEmail(email);
        return customer;
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create customer successfully with valid data")
        void create_WithValidData_ShouldReturnCustomerResponse() {
            //Given
            CustomerRequest request = new CustomerRequest("John Doe", "john@example.com");
            Customer savedCustomer = createCustomer(1L, "John Doe", "john@example.com");
            when(repository.existsByEmailAndDeletedFalse("john@example.com")).thenReturn(false);
            when(repository.save(any(Customer.class))).thenReturn(savedCustomer);

            //When
            CustomerResponse response = customerService.create(request);

            //Then
            assertThat(response).isNotNull();
            assertThat(response.fullName()).isEqualTo("John Doe");
            assertThat(response.email()).isEqualTo("john@example.com");
            verify(repository).existsByEmailAndDeletedFalse("john@example.com");
            verify(repository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when email already exists")
        void create_WithDuplicateEmail_ShouldThrowIllegalStateException() {
            //Given
            CustomerRequest request = new CustomerRequest("John Doe", "john@example.com");
            when(repository.existsByEmailAndDeletedFalse("john@example.com")).thenReturn(true);

            //When/Then
            assertThatThrownBy(() -> customerService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ya existe un cliente con ese email");
            verify(repository).existsByEmailAndDeletedFalse("john@example.com");
            verify(repository, never()).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Find All Customers Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return list of customers when customers exists")
        void findAll_WithExistingCustomers_ShouldReturnList() {
            //Given
            List<Customer> customers = List.of(
                    createCustomer(1L, "John Doe", "john@example.com"),
                    createCustomer(2L, "Jane Smith", "jane@example.com")
            );
            when(repository.findAllByDeletedFalse()).thenReturn(customers);

            //When
            List<CustomerResponse> responses = customerService.findAll();

            //Then
            assertThat(responses)
                    .hasSize(2)
                    .extracting(CustomerResponse::fullName)
                    .containsExactly("John Doe", "Jane Smith");
            verify(repository).findAllByDeletedFalse();
        }

        @Test
        @DisplayName("Should return empty list when no customers exist")
        void findAll_WithNoCustomers_ShouldReturnEmptyList() {
            //Given
            when(repository.findAllByDeletedFalse()).thenReturn(List.of());

            //When
            List<CustomerResponse> responses = customerService.findAll();

            //Then
            assertThat(responses).isEmpty();
            verify(repository).findAllByDeletedFalse();
        }
    }
}
