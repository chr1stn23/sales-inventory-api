package com.christn.salesinventoryapi.dto.request;

import com.christn.salesinventoryapi.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Email(message = "El formato del correo electrónico no es valido")
        String email,

        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        Set<Role> roles,

        Boolean enabled
) {
}
