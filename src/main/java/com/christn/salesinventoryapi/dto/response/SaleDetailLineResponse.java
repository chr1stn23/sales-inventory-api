package com.christn.salesinventoryapi.dto.response;

import java.math.BigDecimal;

public record SaleDetailLineResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {
}
