package com.christn.salesinventoryapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @Schema(description = "Nombre completo del cliente", example = "Juan Pérez", requiredMode =
                Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 150)
        @NotBlank(message = "El nombre completo del cliente no puede estar vacío")
        @Size(max = 150, message = "El nombre completo del cliente no puede superar los 150 caracteres")
        String fullName,

        @Schema(description = "Correo electrónico del cliente", example = "juanperez@example.com", format = "email")
        @NotBlank(message = "El correo electrónico del cliente no puede estar vacío")
        @Email(message = "El correo electrónico del cliente debe tener un formato válido")
        String email
) {
}
