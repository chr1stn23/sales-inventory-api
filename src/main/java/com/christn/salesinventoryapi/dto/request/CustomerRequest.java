package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @Schema(description = "Nombre completo del cliente", example = "Juan Pérez", requiredMode =
                Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 150)
        @NotBlank
        @Size(max = 150)
        String fullName,

        @Schema(description = "Correo electrónico del cliente", example = "juanperez@example.com", format = "email")
        @NotBlank
        @Email
        String email
) {
}
