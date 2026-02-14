package com.christn.salesinventoryapi.dto.response;

import com.christn.salesinventoryapi.model.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummaryResponse(
        Long id,
        LocalDateTime saleDate,
        SaleStatus status,
        BigDecimal totalAmount,
        Long customerId,
        String customerName
) {
}
