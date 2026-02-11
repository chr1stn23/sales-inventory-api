package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreatePurchaseBatchRequest(

        @Size(max = 80, message = "El código del lote debe tener como máximo 80 caracteres")
        String batchCode,

        @Future(message = "La fecha de vencimiento debe ser una fecha futura")
        LocalDateTime expiresAt, // Obligatorio si el producto es perecible (se valida en service)

        @NotNull(message = "La cantidad inicial no puede ser nula")
        @Min(value = 1, message = "La cantidad inicial debe ser mayor o igual a 1")
        Integer quantity
) {
}
