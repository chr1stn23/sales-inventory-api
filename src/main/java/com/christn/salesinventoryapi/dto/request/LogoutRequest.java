package com.christn.salesinventoryapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "El token de refresco no puede estar vacío")
        String refreshToken
) {
}
