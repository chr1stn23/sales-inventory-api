package com.christn.salesinventoryapi.dto.response;

public record SupplierResponse(
        Long id,
        String name,
        String documentNumber,
        String phone,
        String email
) {
}
