package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.Size;

public record VoidSaleRequest (
        @Size(max = 255, message = "El motivo de anulación no puede superar los 255 caracteres")
        String reason
) {
}
