package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.request.VoidSaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.exception.ApiError;
import com.christn.salesinventoryapi.model.SaleStatus;
import com.christn.salesinventoryapi.service.SaleService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales", description = "Registro y consulta de ventas")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService service;

    @Operation(summary = "Crear venta", description = "Registra una nueva venta en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación"),
            @ApiResponse(responseCode = "404", description = "No encontrado: El Cliente o uno de los Productos no " +
                    "existen")
    })
    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        SaleResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/sales/" + response.id())
                .body(response);
    }

    @Operation(summary = "Listar ventas", description = "Obtiene una lista de todas las ventas registradas")
    @GetMapping
    public List<SaleResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Obtener venta por ID", description = "Busca una venta por su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta encontrada"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada", content = @Content(schema =
            @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public SaleResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Buscar venta con filtros", description = "Buscar ventas por ID del cliente, fecha y total")
    @GetMapping("/search")
    public PageResponse<SaleSummaryResponse> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) BigDecimal minTotal,
            @RequestParam(required = false) BigDecimal maxTotal,
            @RequestParam(required = false) SaleStatus status,
            @PageableDefault(sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.search(customerId, from, to, minTotal, maxTotal, status, pageable);
    }

    @Operation(summary = "Anular venta", description = "Anula una venta existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta anulada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "409", description = "Venta ya anulada"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado")
    })
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping("/{id}/void")
    public SaleResponse voidSale(@PathVariable Long id,
            @Valid @RequestBody(required = false) VoidSaleRequest body) {
        String reason = (body != null) ? body.reason() : null;
        return service.voidSale(id, reason);
    }

    @Operation(summary = "Completar venta", description = "Marca la venta como COMPLETED si el total está pagado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta completada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "409", description = "La venta no está totalmente pagado o no ACTIVE")
    })
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping("/{id}/complete")
    public SaleResponse completeSale(@PathVariable Long id) {
        return service.completeSale(id);
    }
}
