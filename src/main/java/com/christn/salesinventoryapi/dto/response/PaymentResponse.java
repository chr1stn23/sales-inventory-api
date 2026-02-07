package com.christn.salesinventoryapi.dto.response;

import com.christn.salesinventoryapi.model.PaymentMethod;
import com.christn.salesinventoryapi.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long saleId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal change,
        LocalDateTime paidAt,
        String reference,
        Long createdByUserId
) {
}
