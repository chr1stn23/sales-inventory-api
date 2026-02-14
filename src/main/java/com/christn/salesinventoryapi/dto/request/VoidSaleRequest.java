package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VoidSaleRequest(

        @NotBlank(message = "El motivo de anulación de venta no puede estar vacío")
        String reason
) {
}
