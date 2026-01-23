package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleDetailRequest(

        @Schema(description = "Identificador del producto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ID del producto no puede ser nulo")
        Long productId,

        @Schema(description = "Cantidad del producto", example = "2", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1")
        @NotNull(message = "La cantidad del producto no puede ser nula")
        @Min(value = 1, message = "La cantidad del producto no puede ser menor que 1")
        Integer quantity
) {

}