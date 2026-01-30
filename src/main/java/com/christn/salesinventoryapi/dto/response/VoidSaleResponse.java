package com.christn.salesinventoryapi.dto.response;

import com.christn.salesinventoryapi.model.SaleStatus;

import java.time.LocalDateTime;

public record VoidSaleResponse(
        Long saleId,
        SaleStatus status,
        LocalDateTime voidedAt,
        String voidedBy,
        Long voidedByUserId,
        String voidReason
) {
}
