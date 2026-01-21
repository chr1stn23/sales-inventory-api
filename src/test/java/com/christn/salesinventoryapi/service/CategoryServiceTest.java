package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CategoryRequest;
import com.christn.salesinventoryapi.dto.response.CategoryResponse;
import com.christn.salesinventoryapi.model.Category;
import com.christn.salesinventoryapi.repository.CategoryRepository;
import com.christn.salesinventoryapi.service.iml.CategoryServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
public class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // Helper method
    private Category createCategory(Long id, String name, String description) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    @Nested
    @DisplayName("Create Category Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create a category successfully with valid data")
        void create_WithValidData_ShouldReturnCategoryResponse() {
            //Given
            CategoryRequest request = new CategoryRequest("Electrónica", "Productos electrónicos");
            Category savedCategory = createCategory(1L, "Electrónica", "Productos electrónicos");

            when(repository.existsByNameAndDeletedFalse("Electrónica")).thenReturn(false);
            when(repository.save(any(Category.class))).thenReturn(savedCategory);

            //When
            CategoryResponse response = categoryService.create(request);

            //Then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Electrónica");
            assertThat(response.description()).isEqualTo("Productos electrónicos");
            verify(repository).existsByNameAndDeletedFalse("Electrónica");
            verify(repository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when category name already exists")
        void create_WithDuplicateName_ShouldThrowIllegalStateException() {
            //Given
            CategoryRequest request = new CategoryRequest("Electrónica", "Productos electrónicos");
            when(repository.existsByNameAndDeletedFalse("Electrónica")).thenReturn(true);

            //When/Then
            assertThatThrownBy(() -> categoryService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ya existe una categoría con ese nombre");

            verify(repository).existsByNameAndDeletedFalse("Electrónica");
            verify(repository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Find All Categories Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return a list of categories when there are existing categories")
        void findAll_WithExistingCategories_ShouldReturnList() {
            //Given
            List<Category> categories = List.of(
                    createCategory(1L, "Electrónica", "Desc 1"),
                    createCategory(2L, "Hogar", "Desc 2")
            );
            when(repository.findAllByDeletedFalse()).thenReturn(categories);

            //When
            List<CategoryResponse> responses = categoryService.findAll();

            //Then
            assertThat(responses)
                    .hasSize(2)
                    .extracting(CategoryResponse::name)
                    .containsExactly("Electrónica", "Hogar");
            verify(repository).findAllByDeletedFalse();
        }

        @Test
        @DisplayName("Should return an empty list when there are no categories")
        void findAll_WithNoCategories_ShouldReturnEmptyList() {
            //Given
            when(repository.findAllByDeletedFalse()).thenReturn(List.of());

            //When
            List<CategoryResponse> responses = categoryService.findAll();

            //Then
            assertThat(responses).isEmpty();
            verify(repository).findAllByDeletedFalse();
        }

        @Test
        @DisplayName("Should only return non-deleted categories")
        void findAll_ShouldOnlyReturnNonDeletedCategories() {
            //Given
            when(repository.findAllByDeletedFalse()).thenReturn(List.of());

            //When
            categoryService.findAll();

            //Then
            verify(repository).findAllByDeletedFalse();
            verify(repository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("Find Category By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return category when id exists")
        void findById_WithExistingId_ShouldReturnCategory() {
            //Given
            Category category = createCategory(1L, "Mascotas", "Desc");
            when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));

            //When
            CategoryResponse response = categoryService.findById(1L);

            //Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Mascotas");
            verify(repository).findByIdAndDeletedFalse(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when id does not exist")
        void findById_WithNonExistingId_ShouldThrowEntityNotFoundException() {
            //Given
            when(repository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> categoryService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Categoría no encontrada");

            verify(repository).findByIdAndDeletedFalse(99L);
        }
    }

    @Nested
    @DisplayName("Update Category Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update category successfully with valid data")
        void update_WithValidData_ShouldReturnUpdatedCategory() {
            //Given
            CategoryRequest request = new CategoryRequest("Updated Name", "Updated Desc");
            Category existingCategory = createCategory(1L, "Old Name", "Old Desc");

            when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingCategory));
            when(repository.existsByNameAndDeletedFalse("Updated Name")).thenReturn(false);

            //When
            CategoryResponse response = categoryService.update(1L, request);

            //Then
            assertThat(response.name()).isEqualTo("Updated Name");
            assertThat(response.description()).isEqualTo("Updated Desc");
            assertThat(existingCategory.getName()).isEqualTo("Updated Name");
            assertThat(existingCategory.getDescription()).isEqualTo("Updated Desc");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating a non-existing category")
        void update_WithNonExistingId_ShouldThrowEntityNotFoundException() {
            //Given
            CategoryRequest request = new CategoryRequest("Updated Name", "Updated Desc");
            when(repository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> categoryService.update(99L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Categoría no encontrada");

            verify(repository).findByIdAndDeletedFalse(99L);
            verify(repository, never()).existsByNameAndDeletedFalse(request.name());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when updating to duplicate name")
        void update_WithDuplicateName_ShouldThrowIllegalStateException() {
            //Given
            CategoryRequest request = new CategoryRequest("Duplicate", "Desc");
            Category existingCategory = createCategory(1L, "Original", "Desc");

            when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingCategory));
            when(repository.existsByNameAndDeletedFalse("Duplicate")).thenReturn(true);

            //When/Then
            assertThatThrownBy(() -> categoryService.update(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ya existe una categoría con ese nombre");
            verify(repository).findByIdAndDeletedFalse(1L);
            verify(repository).existsByNameAndDeletedFalse("Duplicate");
            assertThat(existingCategory.getName()).isEqualTo("Original");
        }

        @Test
        @DisplayName("Should allow updating with same name")
        void update_WithSameName_ShouldNotThrowException() {
            //Given
            CategoryRequest request = new CategoryRequest("Electrónica", "New Desc");
            Category existingCategory = createCategory(1L, "Electrónica", "Old Desc");

            when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingCategory));

            //When/Then
            assertThatCode(() -> categoryService.update(1L, request))
                    .doesNotThrowAnyException();
            assertThat(existingCategory.getDescription()).isEqualTo("New Desc");
            verify(repository, never()).existsByNameAndDeletedFalse(anyString());
        }
    }

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteTest {

        @Test
        @DisplayName("Should soft delete category by setting delete to true")
        void delete_WithExistingId_ShouldSetDeletedTrue() {
            //Given
            Category category = createCategory(1L, "Electrónica", "Desc");
            when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));

            //When
            categoryService.delete(1L);

            //Then
            assertThat(category.getDeleted()).isTrue();
            verify(repository).findByIdAndDeletedFalse(1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existing category")
        void delete_WithNonExistingId_ShouldThrowEntityNotFoundException() {
            //Given
            when(repository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> categoryService.delete(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Categoría no encontrada");
            verify(repository).findByIdAndDeletedFalse(99L);
            verify(repository, never()).save(any());
        }
    }
}
