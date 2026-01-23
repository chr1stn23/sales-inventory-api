package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @Schema(description = "Nombre del producto", example = "Televisor", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(max = 150, message = "El nombre del producto no puede superar los 150 caracteres")
        String name,

        @Schema(description = "Descripción del producto", example = "Televisor de 55 pulgadas")
        @Size(max = 255, message = "La descripción del producto no puede superar los 255 caracteres")
        String description,

        @Schema(description = "Precio del producto", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED
                , minimum = "0", exclusiveMinimum = true)
        @NotNull(message = "El precio del producto no puede ser nulo")
        @Positive(message = "El precio del producto debe ser mayor que 0")
        BigDecimal price,

        @Schema(description = "Cantidad en stock del producto", example = "10", requiredMode =
                Schema.RequiredMode.REQUIRED, minimum = "0")
        @NotNull(message = "El stock del producto no puede ser nulo")
        @Min(value = 0, message = "El stock del producto no puede ser negativo")
        Integer stock,

        @Schema(description = "Identificador de la categoría del producto", example = "1", requiredMode =
                Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ID de categoría del producto no puede ser nulo")
        Long categoryId
) {
}
