package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.CategoryRequest;
import com.christn.salesinventoryapi.dto.response.CategoryResponse;
import com.christn.salesinventoryapi.exception.ApiError;
import com.christn.salesinventoryapi.service.CategoryService;
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
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "CRUD de categorías")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @Operation(summary = "Crear categoría", description = "Registra una nueva categoría en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación", content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Categoría duplicada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/categories/" + response.id())
                .body(response);
    }

    @Operation(summary = "Listar categorías", description = "Obtiene una lista de todas las categorías activas")
    @GetMapping
    public List<CategoryResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Obtener categoría por ID", description = "Busca una categoría por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Actualizar categoría", description = "Actualiza los datos de una categoría existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Categoría duplicada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "Eliminar categoría", description = "Marca una categoría como eliminada (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
