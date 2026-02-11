package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PostPurchaseRequest(
        @NotEmpty(message = "La lista de items no puede estar vacía")
        List<@Valid PostPurchaseItemRequest> items
) {
}

