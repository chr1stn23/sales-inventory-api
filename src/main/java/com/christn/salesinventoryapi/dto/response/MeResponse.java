package com.christn.salesinventoryapi.dto.response;

import java.util.Set;

public record MeResponse(
        Long id,
        String email,
        Set<String> roles
) {
}
