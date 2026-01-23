package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.SaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.exception.InsufficientStockException;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.repository.ProductRepository;
import com.christn.salesinventoryapi.repository.SaleRepository;
import com.christn.salesinventoryapi.service.impl.SaleServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleService Tests")
public class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private SaleServiceImpl saleService;

    //Helper Methods
    private Customer createCustomer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        return customer;
    }

    private Product createProduct(Long id, String name, BigDecimal price, Integer stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        return product;
    }

    @Nested
    @DisplayName("Create Sale Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create sale successfully with valid data")
        void create_WithValidData_ShouldReturnSaleResponse() {
            //Given
            Customer customer = createCustomer(1L);
            Product product1 = createProduct(1L, "Product 1", new BigDecimal("100.00"), 10);
            SaleDetailRequest detailRequest = new SaleDetailRequest(1L, 2);
            SaleRequest request = new SaleRequest(1L, List.of(detailRequest));

            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product1));
            when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
                Sale sale = invocation.getArgument(0);
                sale.setId(1L);
                return sale;
            });

            //When
            SaleResponse response = saleService.create(request);

            //Then
            assertThat(response).isNotNull();
            assertThat(response.totalAmount()).isEqualTo(new BigDecimal("200.00"));
            assertThat(response.details()).hasSize(1);
            assertThat(product1.getStock()).isEqualTo(8);

            verify(saleRepository).save(any(Sale.class));
        }

        @Test
        @DisplayName("Should create sale with multiple products successfully")
        void create_WithMultipleProducts_ShouldReturnSaleResponse() {
            //Given
            Customer customer = createCustomer(2L);
            Product product1 = createProduct(1L, "Product 1", new BigDecimal("50.00"), 10);
            Product product2 = createProduct(2L, "Product 2", new BigDecimal("20.00"), 5);
            SaleRequest request = new SaleRequest(2L,
                    List.of(
                            new SaleDetailRequest(1L, 2),
                            new SaleDetailRequest(2L, 3)
                    )
            );

            when(customerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(product2));
            when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

            //When
            SaleResponse response = saleService.create(request);

            //Then
            assertThat(response.totalAmount()).isEqualTo(new BigDecimal("160.00"));
            assertThat(product1.getStock()).isEqualTo(8);
            assertThat(product2.getStock()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should create sale with grouped products successfully")
        void create_WithDuplicatedProducts_shouldGroupAndSumQuantities() {
            //Given
            Customer customer = createCustomer(1L);
            Product product = createProduct(1L, "Product", new BigDecimal("50.00"), 10);
            SaleRequest request = new SaleRequest(1L,
                    List.of(
                            new SaleDetailRequest(1L, 2),
                            new SaleDetailRequest(1L, 3)
                    )
            );

            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
            when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

            //When
            SaleResponse response = saleService.create(request);

            //Then
            assertThat(response.totalAmount()).isEqualTo(new BigDecimal("250.00"));
            assertThat(response.details()).hasSize(1);
            assertThat(product.getStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when customer does not exist")
        void create_WithInvalidCustomerId_ShouldThrowEntityNotFoundException() {
            //Given
            SaleRequest request = new SaleRequest(99L, List.of());
            when(customerRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> saleService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Cliente no encontrado");
            verify(saleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist")
        void create_WithInvalidProductId_ShouldThrowEntityNotFoundException() {
            //Given
            Customer customer = createCustomer(1L);
            SaleRequest request = new SaleRequest(1L, List.of(new SaleDetailRequest(99L, 1)));
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> saleService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Producto no encontrado");
            verify(saleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InsufficientStockException when stock is insufficient")
        void create_whenStockIsInsufficient_shouldThrowInsufficientStockException() {
            //Given
            Customer customer = createCustomer(1L);
            Product product = createProduct(1L, "Product", new BigDecimal("100.00"), 1);
            SaleRequest request = new SaleRequest(1L, List.of(new SaleDetailRequest(1L, 5)));

            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            //When/Then
            assertThatThrownBy(() -> saleService.create(request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Stock insuficiente del producto " + product.getName());
            verify(saleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InsufficientStockException when stock is insufficient for all products")
        void create_WithDuplicatedProductsAndInsufficientStock_ShouldFail() {
            //Given
            Customer customer = createCustomer(1L);
            Product product = createProduct(1L, "Product", new BigDecimal("100.00"), 6);
            SaleRequest request = new SaleRequest(1L,
                    List.of(
                            new SaleDetailRequest(1L, 5),
                            new SaleDetailRequest(1L, 4)
                    )
            );

            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            //When/Then
            assertThatThrownBy(() -> saleService.create(request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Stock insuficiente del producto " + product.getName());
            verify(saleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when one product exists and the other does not")
        void create_WhenOneProductExistsAndOtherDoesNot_ShouldThrowEntityNotFoundException() {
            // Given
            Customer customer = createCustomer(1L);
            Product product1 = createProduct(1L, "Product 1", new BigDecimal("100.00"), 10);

            SaleRequest request = new SaleRequest(
                    1L,
                    List.of(
                            new SaleDetailRequest(1L, 1), // existe
                            new SaleDetailRequest(2L, 3)  // NO existe
                    )
            );

            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> saleService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Producto no encontrado");

            assertThat(product1.getStock()).isEqualTo(10);
            verify(saleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find All Sales Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return list of sales when sales exist")
        void findAll_WithExistingSales_ShouldReturnList() {
            //Given
            Customer customer = createCustomer(3L);
            Sale sale1 = new Sale();
            sale1.setId(1L);
            sale1.setCustomer(customer);
            Sale sale2 = new Sale();
            sale2.setId(2L);
            sale2.setCustomer(customer);
            when(saleRepository.findAllByDeletedFalse()).thenReturn(List.of(sale1, sale2));

            //When
            List<SaleResponse> responses = saleService.findAll();

            //Then
            assertThat(responses).isNotNull();
            assertThat(responses).hasSize(2);
            assertThat(responses.get(1).customer().id()).isEqualTo(3L);
            verify(saleRepository).findAllByDeletedFalse();
        }

        @Test
        @DisplayName("Should return empty list when no sales exist")
        void findAll_WithNoSales_ShouldReturnEmptyList() {
            //Given
            when(saleRepository.findAllByDeletedFalse()).thenReturn(List.of());

            //When
            List<SaleResponse> responses = saleService.findAll();

            //Then
            assertThat(responses).isNotNull();
            assertThat(responses).isEmpty();
            verify(saleRepository).findAllByDeletedFalse();
        }
    }
}
