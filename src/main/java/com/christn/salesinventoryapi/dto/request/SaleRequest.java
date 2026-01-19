package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaleRequest(

        @NotNull
        Long customerId,

        @NotEmpty
        List<SaleDetailRequest> details
) {
}
