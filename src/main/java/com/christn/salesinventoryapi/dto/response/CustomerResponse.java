package com.christn.salesinventoryapi.dto.response;

public record CustomerResponse(
        Long id,
        String fullName,
        String email
) {
}
