package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleDetailRequest(

        @Schema(description = "Identificador del producto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long productId,

        @Schema(description = "Cantidad del producto", example = "2", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1")
        @NotNull
        @Min(1)
        Integer quantity
) {

}