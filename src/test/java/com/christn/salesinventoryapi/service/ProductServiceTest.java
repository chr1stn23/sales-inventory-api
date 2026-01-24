package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.ProductRequest;
import com.christn.salesinventoryapi.dto.response.ProductResponse;
import com.christn.salesinventoryapi.model.Category;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.repository.CategoryRepository;
import com.christn.salesinventoryapi.repository.ProductRepository;
import com.christn.salesinventoryapi.service.impl.ProductServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
@ActiveProfiles("test")
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    //Helper methods
    private Product createProduct(Long id, String name, String description, BigDecimal price, Integer stock,
            Category category) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        return product;
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private ProductRequest createProductRequest(String name, String description, BigDecimal price,
            Integer stock, Long categoryId) {
        return new ProductRequest(name, description, price, stock, categoryId);
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create product successfully with valid data")
        void create_WithValidData_ShouldReturnProductResponse() {
            //Given
            ProductRequest request = createProductRequest("Laptop", "Gaming Laptop", new BigDecimal("1500.00"), 10, 1L);
            Category category = createCategory(1L, "Electrónica");
            Product savedProduct = createProduct(1L, "Laptop", "Gaming Laptop", new BigDecimal("1500.00"), 10,
                    category);
            when(productRepository.existsByNameAndDeletedFalse("Laptop")).thenReturn(false);
            when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            //When
            ProductResponse response = productService.create(request);

            //Then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Laptop");
            assertThat(response.price()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(response.stock()).isEqualTo(10);
            assertThat(response.category().name()).isEqualTo("Electrónica");

            verify(productRepository).existsByNameAndDeletedFalse("Laptop");
            verify(categoryRepository).findByIdAndDeletedFalse(1L);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when product name already exists")
        void create_WithDuplicateName_ShouldThrowIllegalStateException() {
            //Given
            ProductRequest request = createProductRequest("Laptop", "Gaming Laptop", new BigDecimal("1500.00"), 5, 1L);
            when(productRepository.existsByNameAndDeletedFalse("Laptop")).thenReturn(true);

            //When/Then
            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ya existe un producto con ese nombre");
            verify(productRepository).existsByNameAndDeletedFalse("Laptop");
            verify(categoryRepository, never()).findByIdAndDeletedFalse(anyLong());
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when category does not exist")
        void create_WithInvalidCategoryId_ShouldThrowEntityNotFoundException() {
            //Given
            ProductRequest request = createProductRequest("Laptop", "Description", new BigDecimal("1500.00"), 5, 99L);
            when(productRepository.existsByNameAndDeletedFalse("Laptop")).thenReturn(false);
            when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Categoría no valida");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should associate category correctly when creating product")
        void create_ShouldAssociateCategoryCorrectly() {
            //Given
            ProductRequest request = createProductRequest("Mouse", "Mouse Inalámbrico", new BigDecimal("25.00"), 50,
                    1L);
            Category category = createCategory(1L, "Electrónica");
            when(productRepository.existsByNameAndDeletedFalse("Mouse")).thenReturn(false);
            when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                assertThat(product.getCategory()).isEqualTo(category);
                product.setId(1L);
                return product;
            });

            //When
            productService.create(request);

            //Then
            verify(productRepository).save(argThat(product ->
                    product.getCategory() != null && product.getCategory().getId().equals(1L)
            ));
        }

        @Test
        @DisplayName("Should set deleted to false by default when creating")
        void create_ShouldSetDeletedToFalseByDefault() {
            //Given
            ProductRequest request = createProductRequest("Teclado", "Mecánico", new BigDecimal("80.00"), 5, 1L);
            Category category = createCategory(1L, "Electrónica");
            when(productRepository.existsByNameAndDeletedFalse("Teclado")).thenReturn(false);
            when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                assertThat(product.getDeleted()).isFalse();
                product.setId(1L);
                return product;
            });

            //When
            productService.create(request);

            //Then
            verify(productRepository).save(argThat(product -> !product.getDeleted()));
        }
    }

    @Nested
    @DisplayName("Find All Products Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return list of products when products exist")
        void findAll_WithExistingProducts_ShouldReturnList() {
            //Given
            Category category = createCategory(2L, "Hogar");
            List<Product> products = List.of(
                    createProduct(1L, "Recipiente", "Desc 1", new BigDecimal("25.00"), 7, category),
                    createProduct(2L, "Joyero", "Desc 2", new BigDecimal("6.00"), 10, category)
            );
            when(productRepository.findAllByDeletedFalse()).thenReturn(products);

            //When
            List<ProductResponse> responses = productService.findAll();

            //Then
            assertThat(responses)
                    .hasSize(2)
                    .extracting(ProductResponse::name)
                    .containsExactly("Recipiente", "Joyero");
            verify(productRepository).findAllByDeletedFalse();
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void findAll_WithNoProducts_ShouldReturnEmptyList() {
            //Given
            when(productRepository.findAllByDeletedFalse()).thenReturn(List.of());

            //When
            List<ProductResponse> responses = productService.findAll();

            //Then
            assertThat(responses).isEmpty();
            verify(productRepository).findAllByDeletedFalse();
        }
    }

    @Nested
    @DisplayName("Find All By Category Id Tests")
    class FindAllByCategoryIdTests {

        @Test
        @DisplayName("Should return products for specific category")
        void findAllByCategoryId_WithExistingProducts_ShouldReturnList() {
            //Given
            Category category = createCategory(1L, "Electrónica");
            List<Product> products = List.of(
                    createProduct(1L, "Laptop", "Gaming", new BigDecimal("1500"), 10, category),
                    createProduct(2L, "Mouse", "Inalámbrico", new BigDecimal("25"), 50, category)
            );
            when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
            when(productRepository.findAllByCategoryIdAndDeletedFalse(1L)).thenReturn(products);

            //When
            List<ProductResponse> responses = productService.findAllByCategoryId(1L);

            //Then
            assertThat(responses)
                    .hasSize(2)
                    .allMatch(response -> response.category().name().equals("Electrónica"));
            verify(productRepository).findAllByCategoryIdAndDeletedFalse(1L);
        }

        @Test
        @DisplayName("Should return empty list when category has no products")
        void findAllByCategoryId_WithNoProducts_ShouldReturnEmptyList() {
            //Given
            Category category = createCategory(1L, "Electrónica");
            when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(category));
            when(productRepository.findAllByCategoryIdAndDeletedFalse(1L)).thenReturn(List.of());

            //When
            List<ProductResponse> responses = productService.findAllByCategoryId(1L);

            //Then
            assertThat(responses).isEmpty();
            verify(productRepository).findAllByCategoryIdAndDeletedFalse(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when category does not exist")
        void findAllByCategoryId_WithInvalidCategoryId_ShouldThrowException() {
            //Given
            when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> productService.findAllByCategoryId(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Categoría no encontrada");
            verify(productRepository, never()).findAllByCategoryIdAndDeletedFalse(anyLong());
        }
    }

    @Nested
    @DisplayName("Find By Id Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return product when id exists")
        void findById_WithExistingId_ShouldReturnProduct() {
            //Given
            Category category = createCategory(1L, "Electrónica");
            Product product = createProduct(1L, "Laptop", "Gaming", new BigDecimal("1500"), 10, category);
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            //When
            ProductResponse response = productService.findById(1L);

            //Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Laptop");
            verify(productRepository).findByIdAndDeletedFalse(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when id does not exist")
        void findById_WithNonExistingId_ShouldThrowEntityNotFoundException() {
            //Given
            when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Producto no encontrado");
            verify(productRepository).findByIdAndDeletedFalse(99L);
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update product successfully with valid data")
        void update_WithValidData_ShouldReturnUpdatedProduct() {
            //Given
            Category oldCategory = createCategory(1L, "Electrónica");
            Category newCategory = createCategory(2L, "Cómputo");
            Product existingProduct = createProduct(1L, "Old Name", "Old Desc", new BigDecimal("1500"), 4, oldCategory);
            ProductRequest request = createProductRequest("New Name", "New Desc", new BigDecimal("2000"), 10, 2L);

            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingProduct));
            when(productRepository.existsByNameAndDeletedFalse("New Name")).thenReturn(false);
            when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(newCategory));

            //When
            ProductResponse response = productService.update(1L, request);

            //Then
            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.description()).isEqualTo("New Desc");
            assertThat(response.price()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(response.stock()).isEqualTo(10);
            assertThat(response.category().name()).isEqualTo("Cómputo");

            assertThat(existingProduct.getName()).isEqualTo("New Name");
            assertThat(existingProduct.getDescription()).isEqualTo("New Desc");
            assertThat(existingProduct.getPrice()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(existingProduct.getStock()).isEqualTo(10);
            assertThat(existingProduct.getCategory()).isEqualTo(newCategory);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating non-existing product")
        void update_WithNonExistingId_ShouldThrowEntityNotFoundException() {
            //Given
            ProductRequest request = createProductRequest("Name", "Desc", new BigDecimal("100"), 5, 1L);
            when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> productService.update(99L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Producto no encontrado");
            verify(productRepository, never()).existsByNameAndDeletedFalse(anyString());
            verify(categoryRepository, never()).findByIdAndDeletedFalse(anyLong());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when updating to duplicate name")
        void update_WithDuplicateName_ShouldThrowIllegalStateException() {
            //Given
            Category category = createCategory(1L, "Electrónica");
            Product existingProduct = createProduct(1L, "Original", "Desc", new BigDecimal("100"), 5, category);
            ProductRequest request = createProductRequest("Duplicate", "Desc", new BigDecimal("100"), 5, 1L);
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingProduct));
            when(productRepository.existsByNameAndDeletedFalse("Duplicate")).thenReturn(true);

            //When/Then
            assertThatThrownBy(() -> productService.update(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ya existe un producto con ese nombre");
            verify(categoryRepository, never()).findByIdAndDeletedFalse(anyLong());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when category does not exist")
        void update_WithInvalidCategoryId_ShouldThrowEntityNotFoundException() {
            //Given
            Category oldCategory = createCategory(1L, "Electrónica");
            Product existingProduct = createProduct(1L, "Laptop", "Desc", new BigDecimal("100"), 5, oldCategory);
            ProductRequest request = createProductRequest("Laptop", "Desc", new BigDecimal("100"), 5, 99L);
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingProduct));
            when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> productService.update(1L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Categoría no encontrada");
        }

        @Test
        @DisplayName("Should allow updating with same name")
        void update_WithSameName_ShouldNotThrowException() {
            //Given
            Category category = createCategory(1L, "Electrónica");
            Product existingProduct = createProduct(1L, "Laptop", "Old Desc", new BigDecimal("100"), 5, category);
            ProductRequest request = createProductRequest("Laptop", "New Desc", new BigDecimal("200"), 10, 1L);
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingProduct));

            //When/Then
            assertThatCode(() -> productService.update(1L, request))
                    .doesNotThrowAnyException();
            assertThat(existingProduct.getDescription()).isEqualTo("New Desc");
            assertThat(existingProduct.getPrice()).isEqualByComparingTo(new BigDecimal("200"));
            verify(productRepository, never()).existsByNameAndDeletedFalse(anyString());
            verify(categoryRepository, never()).findByIdAndDeletedFalse(anyLong());
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteTest {

        @Test
        @DisplayName("Should soft delete product by setting delete to true")
        void delete_WithExistingId_ShouldSetDeletedTrue() {
            //Given
            Category category = createCategory(1L, "Electrónica");
            Product product = createProduct(1L, "Laptop", "Desc", new BigDecimal("100"), 5, category);
            when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));

            //When
            productService.delete(1L);

            //Then
            assertThat(product.getDeleted()).isTrue();
            verify(productRepository).findByIdAndDeletedFalse(1L);
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existing product")
        void delete_WithNonExistingId_ShouldThrowEntityNotFoundException() {
            //Given
            when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> productService.delete(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Producto no encontrado");
        }
    }
}
