package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.ProductRequest;
import com.christn.salesinventoryapi.dto.response.ProductResponse;
import com.christn.salesinventoryapi.exception.ApiError;
import com.christn.salesinventoryapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "CRUD de productos")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @Operation(summary = "Crear producto", description = "Registra un nuevo producto en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación", content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado: La Categoría no existe", content =
            @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Producto duplicada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/products/" + response.id())
                .body(response);
    }

    @Operation(summary = "Listar productos", description = "Obtiene una lista de todos los productos activos")
    @GetMapping
    public List<ProductResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Listar productos por categoría", description = "Obtiene todos los productos activos de una " +
            "categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productos encontrados"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> findByCategoryId(@PathVariable Long categoryId) {
        return service.findAllByCategoryId(categoryId);
    }

    @Operation(summary = "Obtener producto por ID", description = "Busca un producto por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Actualizar producto", description = "Actualiza los datos de un producto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación", content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado: El Producto o la Categoría no existen",
                    content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Producto duplicado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "Eliminar producto", description = "Marca un producto como eliminado (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
