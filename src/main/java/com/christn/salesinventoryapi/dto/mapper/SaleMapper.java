package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.SaleDetailLineResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.model.Sale;

public class SaleMapper {

    public static SaleResponse toResponse(Sale sale) {
        var c = sale.getCustomer();

        var details = sale.getDetails().stream()
                .map(d -> new SaleDetailLineResponse(
                        d.getId(),
                        d.getProduct().getId(),
                        d.getProduct().getName(),
                        d.getQuantity(),
                        d.getUnitPrice(),
                        d.getSubTotal()
                ))
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getStatus(),
                c != null ? c.getId() : null,
                c != null ? c.getFullName() : null,
                sale.getTotalAmount(),
                sale.getCreatedAt(),
                sale.getCreatedByUserId(),
                sale.getPostedAt(),
                sale.getPostedByUserId(),
                sale.getCompletedAt(),
                sale.getCompletedByUserId(),
                sale.getVoidedAt(),
                sale.getVoidedByUserId(),
                sale.getVoidReason(),
                details
        );
    }

    public static SaleSummaryResponse toSummaryResponse(Sale sale) {
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getStatus(),
                sale.getTotalAmount(),
                sale.getCustomer().getId(),
                sale.getCustomer().getFullName()
        );
    }
}
