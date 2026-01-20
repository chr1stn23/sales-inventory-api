package com.christn.salesinventoryapi.exception;

import java.time.LocalDateTime;

public record ApiError(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        LocalDateTime timestamp
) {
}
