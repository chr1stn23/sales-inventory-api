package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VoidPurchaseRequest(

        @NotBlank(message = "El campo 'reason' es requerido")
        String reason
) {
}
