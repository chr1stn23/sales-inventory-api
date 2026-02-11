package com.christn.salesinventoryapi.dto.request;

import com.christn.salesinventoryapi.model.PurchaseDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreatePurchaseRequest(
        Long supplierId,

        LocalDateTime purchaseDate,

        @NotNull(message = "El tipo de documento no puede ser nulo")
        PurchaseDocumentType documentType,

        @Size(max = 60, message = "El número de documento debe tener como máximo 60 caracteres")
        String documentNumber,

        String notes,

        @NotEmpty(message = "La lista de items no puede estar vacía")
        List<@Valid CreatePurchaseItemRequest> items
) {
}

