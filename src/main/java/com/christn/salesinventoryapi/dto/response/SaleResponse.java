package com.christn.salesinventoryapi.dto.response;

import com.christn.salesinventoryapi.model.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        LocalDateTime saleDate,
        BigDecimal totalAmount,
        CustomerResponse customer,
        List<SaleDetailResponse> details,
        SaleStatus status
) {
}
