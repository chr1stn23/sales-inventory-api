package com.christn.salesinventoryapi.dto.response;

import com.christn.salesinventoryapi.model.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        LocalDateTime saleDate,
        SaleStatus status,
        Long customerId,
        String customerName,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        Long createdByUserId,
        LocalDateTime postedAt,
        Long postedByUserId,
        LocalDateTime completedAt,
        Long completedByUserId,
        LocalDateTime voidedAt,
        Long voidedByUserId,
        String voidReason,
        List<SaleDetailLineResponse> details
) {
}
