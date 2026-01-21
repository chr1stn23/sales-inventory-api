package com.christn.salesinventoryapi.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ApiError(
        @Schema(description = "URL del tipo de error", example = "https://christn.com/errors/404")
        String type,

        @Schema(description = "Título del error", example = "Not Found")
        String title,

        @Schema(description = "Código de estado HTTP", example = "404")
        int status,

        @Schema(description = "Detalles del error", example = "El producto con ID 4 no se pudo encontrar")
        String detail,

        @Schema(description = "Ruta donde ocurrió el error", example = "/api/products/4")
        String instance,

        @Schema(description = "Fecha y hora del error")
        LocalDateTime timestamp
) {
}
