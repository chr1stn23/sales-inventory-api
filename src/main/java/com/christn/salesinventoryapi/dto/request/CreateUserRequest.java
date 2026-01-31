package com.christn.salesinventoryapi.dto.request;

import com.christn.salesinventoryapi.model.Role;
import jakarta.validation.constraints.*;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "El formato del correo electrónico no es valido")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotEmpty(message = "Los roles no pueden estar vacíos")
        Set<@NotNull Role> roles,

        Boolean enabled
) {
}
