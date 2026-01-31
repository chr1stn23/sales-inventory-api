package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "El correo electrónico debe tener un formato válido")
        @NotBlank(message = "El correo electrónico no puede estar vacío")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía")
        String password
) {
}
