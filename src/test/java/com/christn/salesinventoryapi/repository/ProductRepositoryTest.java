package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Category;
import com.christn.salesinventoryapi.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private Product createProduct(String name, Category category, boolean deleted) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Test Product");
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(10);
        product.setCategory(category);
        product.setDeleted(deleted);
        return productRepository.save(product);
    }

    @Test
    @DisplayName("findAllByCategoryIdAndDeletedFalse should return only active products of the category")
    void findAllByCategoryIdAndDeletedFalse_ShouldReturnOnlyActiveProducts() {
        //Given
        Category cat1 = createCategory("Electrónica");
        Category cat2 = createCategory("Libros");

        createProduct("Laptop", cat1, false);
        createProduct("Mouse", cat1, true); // soft deleted
        createProduct("Teclado", cat2, false);

        //When
        List<Product> result = productRepository.findAllByCategoryIdAndDeletedFalse(cat1.getId());

        //Then
        assertThat(result)
                .hasSize(1)
                .allMatch(product -> !product.getDeleted())
                .allMatch(product -> product.getCategory().getId().equals(cat1.getId()));
    }

    @Test
    @DisplayName("existsByNameAndDeletedFalse should return true only for active products")
    void existsByNameAndDeletedFalse_shouldWorkCorrectly() {
        // Given
        Category category = createCategory("Electrónica");
        createProduct("Laptop", category, false);
        createProduct("Mouse", category, true);

        // Then
        assertThat(productRepository.existsByNameAndDeletedFalse("Laptop")).isTrue();
        assertThat(productRepository.existsByNameAndDeletedFalse("Mouse")).isFalse();
    }

    @Nested
    @DisplayName("findByIdAndDeletedFalse")
    class FindByIdAndDeletedFalseTests {
        @Test
        @DisplayName("Should return product when active")
        void findByIdAndDeletedFalse_shouldReturnProduct_whenActive() {
            // Given
            Category category = createCategory("Electrónica");
            Product product = createProduct("Laptop", category, false);

            // When
            Optional<Product> result = productRepository.findByIdAndDeletedFalse(product.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Laptop");
        }

        @Test
        @DisplayName("Should return empty when product is deleted")
        void findByIdAndDeletedFalse_shouldReturnEmpty_whenDeleted() {
            // Given
            Category category = createCategory("Electrónica");
            Product product = createProduct("Laptop", category, true);

            // When
            Optional<Product> result = productRepository.findByIdAndDeletedFalse(product.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }
}
