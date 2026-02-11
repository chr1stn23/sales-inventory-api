package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePurchaseItemRequest(

        @NotNull(message = "El ID del producto no puede ser nulo")
        Long productId,

        @NotNull(message = "El costo unitario no puede ser nulo")
        @DecimalMin(value = "0.00", message = "El costo unitario debe ser mayor o igual a 0")
        BigDecimal unitCost,

        @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
        Integer quantity
) {
}
