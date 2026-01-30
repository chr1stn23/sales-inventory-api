package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.SaleDetailResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleDetail;

import java.util.List;

public class SaleMapper {

    public static SaleResponse toResponse(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getTotalAmount(),
                CustomerMapper.toResponse(sale.getCustomer()),
                toDetailResponse(sale.getDetails()),
                sale.getStatus(),
                sale.getVoidedAt(),
                sale.getVoidedBy(),
                sale.getVoidedByUserId(),
                sale.getVoidReason()
        );
    }

    private static List<SaleDetailResponse> toDetailResponse(List<SaleDetail> details) {
        return details.stream().map(detail ->
                new SaleDetailResponse(
                        detail.getProduct().getId(),
                        detail.getProduct().getName(),
                        detail.getQuantity(),
                        detail.getUnitPrice(),
                        detail.getSubTotal()
                )
        ).toList();
    }

    public static SaleSummaryResponse toSummaryResponse(Sale sale) {
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getTotalAmount(),
                CustomerMapper.toResponse(sale.getCustomer())
        );
    }
}
