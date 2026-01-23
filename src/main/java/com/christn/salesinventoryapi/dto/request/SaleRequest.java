package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaleRequest(

        @Schema(description = "Identificador del cliente", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ID del cliente no puede ser nulo")
        Long customerId,

        @Schema(description = "Lista de productos incluidos en la venta. Debe contener al menos un detalle",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La lista de detalles de la venta no puede estar vacía")
        @Valid
        List<SaleDetailRequest> details
) {
}
