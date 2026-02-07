package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.PaymentResponse;
import com.christn.salesinventoryapi.model.Payment;

public class PaymentMapper {

    public static PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getSale().getId(),
                p.getAmount(),
                p.getMethod(),
                p.getStatus(),
                p.getChange(),
                p.getPaidAt(),
                p.getReference(),
                p.getCreatedByUser() != null ? p.getCreatedByUser().getId() : null
        );
    }
}
