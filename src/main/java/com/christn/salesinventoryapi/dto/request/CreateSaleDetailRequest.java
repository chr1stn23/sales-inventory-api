package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSaleDetailRequest(

        @Schema(description = "Identificador del producto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ID del producto no puede ser nulo")
        Long productId,

        @Schema(description = "Cantidad del producto", example = "2", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1")
        @NotNull(message = "La cantidad del producto no puede ser nula")
        @Positive(message = "La cantidad del producto debe ser mayor que 0")
        Integer quantity
) {

}