package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Category;
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
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category createCategory(String name, boolean deleted) {
        Category category = new Category();
        category.setName(name);
        category.setDeleted(deleted);
        return categoryRepository.save(category);
    }

    @Test
    @DisplayName("findAllByDeletedFalse should return only active categories")
    void findAllByDeletedFalse_shouldReturnOnlyActiveCategories() {
        // Given
        createCategory("Electrónica", false);
        createCategory("Ropa", true); //soft deleted
        createCategory("Hogar", false);

        // When
        List<Category> result = categoryRepository.findAllByDeletedFalse();

        // Then
        assertThat(result)
                .hasSize(2)
                .allMatch(category -> !category.getDeleted());
    }

    @Test
    @DisplayName("existsByNameAndDeletedFalse should return true only for active category")
    void existsByNameAndDeletedFalse_shouldWorkCorrectly() {
        // Given
        createCategory("Electrónica", false);
        createCategory("Ropa", true);

        // Then
        assertThat(categoryRepository.existsByNameAndDeletedFalse("Electrónica")).isTrue();
        assertThat(categoryRepository.existsByNameAndDeletedFalse("Ropa")).isFalse();
        assertThat(categoryRepository.existsByNameAndDeletedFalse("Inexistente")).isFalse();
    }

    @Nested
    @DisplayName("findByIdAndDeletedFalse")
    class FindByIdAndDeletedFalseTests {

        @Test
        @DisplayName("Should return category when active")
        void findByIdAndDeletedFalse_shouldReturnCategory_whenActive() {
            // Given
            Category category = createCategory("Electrónica", false);

            // When
            Optional<Category> result =
                    categoryRepository.findByIdAndDeletedFalse(category.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Electrónica");
        }

        @Test
        @DisplayName("Should return empty when category is deleted")
        void findByIdAndDeletedFalse_shouldReturnEmpty_whenDeleted() {
            // Given
            Category category = createCategory("Electrónica", true);

            // When
            Optional<Category> result =
                    categoryRepository.findByIdAndDeletedFalse(category.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }
}
