package com.christn.salesinventoryapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummaryResponse(
        Long id,
        LocalDateTime saleDate,
        BigDecimal totalAmount,
        CustomerResponse customer
) {
}
