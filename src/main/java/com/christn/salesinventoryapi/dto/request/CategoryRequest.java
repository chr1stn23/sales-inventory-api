package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Schema(description = "Nombre de la categoría", example = "Electrónica", requiredMode =
                Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Descripción de la categoría", example = "Productos electrónicos", maxLength = 255)
        @Size(max = 255)
        String description
) {
}
