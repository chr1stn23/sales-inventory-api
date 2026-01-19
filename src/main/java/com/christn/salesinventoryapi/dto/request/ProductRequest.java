package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String description,

        @NotNull
        @Positive
        BigDecimal price,

        @NotNull
        @Min(0)
        Integer stock,

        @NotNull
        Long categoryId
) {
}
