package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.exception.ApiError;
import com.christn.salesinventoryapi.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Gestión de clientes")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "409", description = "Cliente duplicado (email)")
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/customers/" + response.id())
                .body(response);
    }

    @Operation(summary = "Listar clientes", description = "Obtiene una lista de todos los clientes registrados")
    @GetMapping
    public List<CustomerResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Obtener cliente por ID", description = "Busca un cliente por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación", content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema =
            @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Cliente duplicado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @PutMapping("{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Eliminar cliente", description = "Marca un cliente como eliminado (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restaurar cliente", description = "Restaura un cliente eliminado (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente restaurado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        service.restore(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar clientes con filtros", description = "Busca clientes por nombre o correo")
    @GetMapping("/search")
    public PageResponse<CustomerResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.search(name, email, pageable);
    }
}
