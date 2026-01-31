package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.ProductRequest;
import com.christn.salesinventoryapi.dto.response.CategoryResponse;
import com.christn.salesinventoryapi.dto.response.ProductResponse;
import com.christn.salesinventoryapi.auth.JwtAuthFilter;
import com.christn.salesinventoryapi.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ProductService productService;

    @Nested
    @DisplayName("POST /api/products")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 when Product is created")
        void create_ShouldReturn201() throws Exception {
            //Given
            CategoryResponse categoryResponse = new CategoryResponse(1L, "Electrónica", "Productos electrónicos");
            ProductRequest request = new ProductRequest("Laptop", "Desc", new BigDecimal("1500.00"), 10, 1L);
            ProductResponse response = new ProductResponse(1L, "Laptop", "Desc", new BigDecimal("1500.00"), 10,
                    categoryResponse);
            //When
            when(productService.create(any())).thenReturn(response);

            //Then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/products/1"))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.category.id").value(1L));
        }

        @ParameterizedTest
        @CsvSource({
                "'',Desc,1500,10,1,El nombre no puede estar vacío",
                "Laptop,'',0,10,1,El precio del producto debe ser mayor que 0",
                "Laptop,Desc,-100,10,1,El precio del producto debe ser mayor que 0",
                "Laptop,Desc,1500,-5,1,El stock del producto no puede ser negativo",
                "Laptop,Desc,1500,10,,El ID de categoría del producto no puede ser nulo",
        })
        void create_ShouldReturn400_WhenInvalidRequest(String name, String description, BigDecimal price, Integer stock,
                Long categoryId, String expectedMessage) throws Exception {
            //Given
            ProductRequest request = new ProductRequest(name, description, price, stock, categoryId);

            //When/Then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)
                            )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(containsString(expectedMessage)))
                    .andExpect(jsonPath("$.instance").value("/api/products"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(productService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 404 when category does not exist")
        void create_ShouldReturn404_WhenInvalidCategoryId() throws Exception {
            //Given
            ProductRequest request = new ProductRequest("Laptop", "Desc", new BigDecimal("1500.00"), 10, 99L);

            //When
            when(productService.create(request)).thenThrow(new EntityNotFoundException("Categoría no encontrada"));

            //Then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.detail").value("Categoría no encontrada"))
                    .andExpect(jsonPath("$.instance").value("/api/products"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 409 when duplicate product")
        void create_ShouldReturn409_WhenDuplicateProduct() throws Exception {
            //Given
            ProductRequest request = new ProductRequest("Laptop", "Desc", new BigDecimal("1500.00"), 10, 1L);

            //When
            when(productService.create(request)).thenThrow(new IllegalStateException("Ya existe un producto con ese " +
                    "nombre"));

            //Then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"))
                    .andExpect(jsonPath("$.detail").value("Ya existe un producto con ese nombre"))
                    .andExpect(jsonPath("$.instance").value("/api/products"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/products")
    class GetAllProducts {

        @Test
        @DisplayName("Should return a list of products when there are existing products")
        void getAll_ShouldReturnList() throws Exception {
            //Given
            CategoryResponse categoryResponse = new CategoryResponse(1L, "Electrónica", "Productos electrónicos");
            ProductResponse response = new ProductResponse(1L, "Laptop", "Desc", new BigDecimal("1500.00"), 10,
                    categoryResponse);
            //When
            when(productService.findAll()).thenReturn(List.of(response));

            //Then
            mockMvc.perform(get("/api/products")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].category.id").value(1L));
        }

        @Test
        @DisplayName("Should return an empty list when there are no products")
        void getAll_ShouldReturnEmptyList() throws Exception {
            //When
            when(productService.findAll()).thenReturn(List.of());

            //Then
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("GET /api/products/category/{categoryId}")
    class GetByCategoryTests {

        @Test
        @DisplayName("Should return list of products for a given category")
        void getByCategory_ShouldReturnList() throws Exception {
            CategoryResponse category = new CategoryResponse(1L, "Electrónica", "Productos electrónicos");
            ProductResponse product = new ProductResponse(1L, "Laptop", "Desc", new BigDecimal("1500.00"), 10,
                    category);

            when(productService.findAllByCategoryId(1L)).thenReturn(List.of(product));

            mockMvc.perform(get("/api/products/category/{categoryId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].category.id").value(1L));
        }

        @Test
        @DisplayName("Should return empty list when category has no products")
        void getByCategory_ShouldReturnEmptyList() throws Exception {
            when(productService.findAllByCategoryId(2L)).thenReturn(List.of());

            mockMvc.perform(get("/api/products/category/{categoryId}", 2L))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("Should return 404 when category does not exist")
        void getByCategory_ShouldReturn404_WhenCategoryNotFound() throws Exception {
            when(productService.findAllByCategoryId(99L))
                    .thenThrow(new EntityNotFoundException("Categoría no encontrada"));

            mockMvc.perform(get("/api/products/category/{categoryId}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Categoría no encontrada"));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetById {
        @Test
        @DisplayName("Should return product when id exists")
        void getById_ShouldReturnProductResponse() throws Exception {
            //Given
            CategoryResponse categoryResponse = new CategoryResponse(1L, "Electrónica", "Productos electrónicos");
            ProductResponse response = new ProductResponse(1L, "Laptop", "Desc", new BigDecimal("1500.00"), 10,
                    categoryResponse);

            //When
            when(productService.findById(1L)).thenReturn(response);

            //Then
            mockMvc.perform(get("/api/products/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.price").value(1500.00))
                    .andExpect(jsonPath("$.category.id").value(1L))
            ;
        }

        @Test
        @DisplayName("Should return 404 when product not found")
        void getById_ShouldReturn404() throws Exception {
            //When
            when(productService.findById(99L)).thenThrow(new EntityNotFoundException("Producto no encontrado"));

            //Then
            mockMvc.perform(get("/api/products/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.detail").value("Producto no encontrado"))
                    .andExpect(jsonPath("$.instance").value("/api/products/99"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 when Product is updated")
        void update_ShouldReturn200() throws Exception {
            //Given
            ProductRequest request = new ProductRequest("Name updated", "Desc updated", new BigDecimal("1500.00"), 10
                    , 2L);
            CategoryResponse categoryResponse = new CategoryResponse(2L, "Electrónica", "Productos electrónicos");
            ProductResponse response = new ProductResponse(1L, "Laptop", "Desc", new BigDecimal("1500.00"), 10,
                    categoryResponse);

            //When
            when(productService.update(any(), any())).thenReturn(response);

            //Then
            mockMvc.perform(put("/api/products/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.category.id").value(2L));
        }

        @Test
        @DisplayName("Should return 400 when invalid request")
        void update_ShouldReturn400_WhenInvalidRequest() throws Exception {
            //Given
            ProductRequest request = new ProductRequest(null, null, null, null, null);

            //When/Then
            mockMvc.perform(put("/api/products/{id}", 2L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").value("/api/products/2"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(productService, never()).update(any(), any());
        }

        @ParameterizedTest
        @CsvSource({"Producto no encontrado", "Categoría no encontrada"})
        @DisplayName("Should return 404 when product or category not found")
        void update_ShouldReturn404_WhenProductOrCategoryNotFound(String errorMessage) throws Exception {
            //Given
            ProductRequest request = new ProductRequest("Laptop", "Desc", new BigDecimal("2000.00"), 15, 3L);

            //When
            when(productService.update(any(), any())).thenThrow(new EntityNotFoundException(errorMessage));

            //Then
            mockMvc.perform(put("/api/products/{id}", 3L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.detail").value(errorMessage))
                    .andExpect(jsonPath("$.instance").value("/api/products/3"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 409 when duplicate product")
        void update_ShouldReturn409_WhenDuplicateProduct() throws Exception {
            //Given
            ProductRequest request = new ProductRequest("Laptop", "Desc", new BigDecimal("1500.00"), 10, 1L);

            //When
            when(productService.update(any(), any())).thenThrow(new IllegalStateException("Ya existe un producto con " +
                    "ese nombre"));

            //Then
            mockMvc.perform(put("/api/products/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"))
                    .andExpect(jsonPath("$.detail").value("Ya existe un producto con ese nombre"))
                    .andExpect(jsonPath("$.instance").value("/api/products/1"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Should delete product and return 204")
        void delete_ShouldReturn204() throws Exception {
            //When
            doNothing().when(productService).delete(1L);

            //Then
            mockMvc.perform(delete("/api/products/{id}", 1L))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            verify(productService, times(1)).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when product not found")
        void delete_ShouldReturn404() throws Exception {
            //When
            doThrow(new EntityNotFoundException("Producto no encontrado")).when(productService).delete(99L);

            //Then
            mockMvc.perform(delete("/api/products/{id}", 99L)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.detail").value("Producto no encontrado"))
                    .andExpect(jsonPath("$.instance").value("/api/products/99"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

        }
    }
}
