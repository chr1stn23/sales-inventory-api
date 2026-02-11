package com.christn.salesinventoryapi.dto.response;

import com.christn.salesinventoryapi.model.PurchaseDocumentType;
import com.christn.salesinventoryapi.model.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseResponse(
        Long id,
        LocalDateTime purchaseDate,
        PurchaseStatus status,
        PurchaseDocumentType documentType,
        String documentNumber,
        String notes,
        Long createdById,

        SupplierResponse supplier,

        BigDecimal totalAmount,

        LocalDateTime postedAt,
        Long postedById,

        LocalDateTime voidedAt,
        Long voidedById,
        String voidReason,

        List<PurchaseItemResponse> items
) {
}
