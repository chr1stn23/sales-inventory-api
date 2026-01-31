package com.christn.salesinventoryapi.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        boolean enabled,
        Set<String> roles,
        LocalDateTime createdAt
) {
}
