package com.christn.salesinventoryapi.dto.response;

import java.math.BigDecimal;

public record SaleDetailResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {
}
