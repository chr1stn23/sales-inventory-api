package com.christn.salesinventoryapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductBatchResponse(
        Long id,
        String batchCode,
        LocalDateTime receivedAt,
        LocalDateTime expiresAt,
        Integer qtyInitial,
        Integer qtyAvailable,
        BigDecimal unitCost
) {
}
