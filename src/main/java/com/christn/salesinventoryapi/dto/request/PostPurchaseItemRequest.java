package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PostPurchaseItemRequest(
        @NotNull
        Long purchaseItemId,

        List<@Valid CreatePurchaseBatchRequest> batches // Si perecible obligatorio y expiresAt obligatorio
) {
}
