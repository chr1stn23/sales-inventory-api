package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleDetailRequest(
        @NotNull
        Long productId,

        @NotNull
        @Min(1)
        Integer quantity
) {

}