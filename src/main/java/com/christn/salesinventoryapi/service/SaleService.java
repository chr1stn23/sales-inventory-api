package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CreateSaleRequest;
import com.christn.salesinventoryapi.dto.request.PostSaleRequest;
import com.christn.salesinventoryapi.dto.request.VoidSaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.model.SaleStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SaleService {

    // 1) Crea una venta en DRAFT (no mueve stock, no crea inventory movement)
    SaleResponse createDraft(CreateSaleRequest request);

    // 2) Publica/activa la venta (DRAFT -> ACTIVE)
    // - aplica FEFO (o consume lotes manuales del request)
    // - descuenta batches + stock
    // - crea InventoryMovement OUT (SALE_OUT)
    // - idempotente: si ya ACTIVE/COMPLETED, devuelve la venta actual
    SaleResponse postSale(Long saleId, PostSaleRequest request);

    // 3) Completa la venta (ACTIVE -> COMPLETED)
    // - regla: pagos posteados >= total
    // - no mueve inventario
    SaleResponse completeSale(Long saleId);

    // 4) Anula venta
    // - si DRAFT: solo pasa a VOIDED (sin reversa de inventario)
    // - si ACTIVE: revierte allocations + batches + stock, movement IN (SALE_VOID_IN)
    // - si COMPLETED: bloqueado (solo ADMIN a futuro)
    SaleResponse voidSale(Long saleId, VoidSaleRequest request);

    // Lectura
    SaleResponse getById(Long saleId);

    // Búsqueda paginada (summary)
    PageResponse<SaleSummaryResponse> search(
            Long customerId,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minTotal,
            BigDecimal maxTotal,
            SaleStatus status,
            Pageable pageable
    );
}
