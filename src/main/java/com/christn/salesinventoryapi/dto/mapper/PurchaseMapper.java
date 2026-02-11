package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.ProductBatchResponse;
import com.christn.salesinventoryapi.dto.response.PurchaseItemResponse;
import com.christn.salesinventoryapi.dto.response.PurchaseResponse;
import com.christn.salesinventoryapi.dto.response.SupplierResponse;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.model.ProductBatch;
import com.christn.salesinventoryapi.model.Purchase;
import com.christn.salesinventoryapi.model.PurchaseItem;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class PurchaseMapper {

    public static PurchaseResponse toResponseSimple(Purchase purchase) {
        SupplierResponse supplier = null;
        if (purchase.getSupplier() != null) {
            var s = purchase.getSupplier();
            supplier = new SupplierResponse(s.getId(), s.getName(), s.getDocumentNumber(), s.getPhone(), s.getEmail());
        }

        List<PurchaseItemResponse> items = purchase.getItems().stream()
                .map(PurchaseMapper::toItemResponseNoBatches)
                .toList();

        return getPurchaseResponse(purchase, supplier, items);
    }

    public static PurchaseResponse toResponseDetail(Purchase purchase, Map<Long, List<ProductBatch>> batchesByItemId) {
        SupplierResponse supplier = null;
        if (purchase.getSupplier() != null) {
            var s = purchase.getSupplier();
            supplier = new SupplierResponse(s.getId(), s.getName(), s.getDocumentNumber(), s.getPhone(), s.getEmail());
        }

        List<PurchaseItemResponse> items = purchase.getItems().stream()
                .map(i -> toItemResponseWithBatches(i, batchesByItemId.getOrDefault(i.getId(), List.of())))
                .toList();

        return getPurchaseResponse(purchase, supplier, items);
    }

    @NonNull
    private static PurchaseResponse getPurchaseResponse(Purchase purchase, SupplierResponse supplier,
            List<PurchaseItemResponse> items) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseDate(),
                purchase.getStatus(),
                purchase.getDocumentType(),
                purchase.getDocumentNumber(),
                purchase.getNotes(),
                purchase.getCreatedByUser() != null ? purchase.getCreatedByUser().getId() : null,
                supplier,
                purchase.getTotalAmount(),
                purchase.getPostedAt(),
                purchase.getPostedByUser() != null ? purchase.getPostedByUser().getId() : null,
                purchase.getVoidedAt(),
                purchase.getVoidedByUser() != null ? purchase.getVoidedByUser().getId() : null,
                purchase.getVoidReason(),
                items
        );
    }

    private static PurchaseItemResponse toItemResponseNoBatches(PurchaseItem item) {
        Product p = item.getProduct();
        return new PurchaseItemResponse(
                item.getId(),
                p.getId(),
                p.getName(),
                p.getPerishable(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getSubTotal(),
                List.of()
        );
    }

    private static PurchaseItemResponse toItemResponseWithBatches(PurchaseItem item, List<ProductBatch> batches) {
        Product p = item.getProduct();

        List<ProductBatchResponse> batchResponses = batches.stream()
                .map(b -> new ProductBatchResponse(
                        b.getId(),
                        b.getBatchCode(),
                        b.getReceivedAt(),
                        b.getExpiresAt(),
                        b.getQtyInitial(),
                        b.getQtyAvailable(),
                        b.getUnitCost()
                ))
                .toList();

        return new PurchaseItemResponse(
                item.getId(),
                p.getId(),
                p.getName(),
                p.getPerishable(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getSubTotal(),
                batchResponses
        );
    }
}