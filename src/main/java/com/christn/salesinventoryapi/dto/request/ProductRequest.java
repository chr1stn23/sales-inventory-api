package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @Schema(description = "Nombre del producto", example = "Televisor", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 150)
        String name,

        @Schema(description = "Descripción del producto", example = "Televisor de 55 pulgadas")
        @Size(max = 255)
        String description,

        @Schema(description = "Precio del producto", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED
                , minimum = "0", exclusiveMinimum = true)
        @NotNull
        @Positive
        BigDecimal price,

        @Schema(description = "Cantidad en stock del producto", example = "10", requiredMode =
                Schema.RequiredMode.REQUIRED, minimum = "0")
        @NotNull
        @Min(0)
        Integer stock,

        @Schema(description = "Identificador de la categoría del producto", example = "1", requiredMode =
                Schema.RequiredMode.REQUIRED)
        @NotNull
        Long categoryId
) {
}
