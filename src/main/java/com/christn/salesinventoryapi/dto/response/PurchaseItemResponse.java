package com.christn.salesinventoryapi.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseItemResponse(
        Long id,
        Long productId,
        String productName,
        Boolean perishable,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal subtotal,

        //Puede estar vacío si está en DRAFT y no se postea
        List<ProductBatchResponse> batches
) {
}
