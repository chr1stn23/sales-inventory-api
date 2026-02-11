package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.CreatePurchaseRequest;
import com.christn.salesinventoryapi.dto.request.PostPurchaseRequest;
import com.christn.salesinventoryapi.dto.request.VoidPurchaseRequest;
import com.christn.salesinventoryapi.dto.response.PurchaseResponse;
import com.christn.salesinventoryapi.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @PostMapping()
    public ResponseEntity<PurchaseResponse> createDraft(@Valid @RequestBody CreatePurchaseRequest request) {
        PurchaseResponse response = purchaseService.createDraft(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @PostMapping("/{id}/post")
    public ResponseEntity<PurchaseResponse> postPurchase(
            @PathVariable Long id,
            @Valid @RequestBody PostPurchaseRequest request) {
        PurchaseResponse response = purchaseService.postPurchase(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','SELLER')")
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> getById(@PathVariable Long id) {
        PurchaseResponse response = purchaseService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @PostMapping("/{id}/void")
    public ResponseEntity<PurchaseResponse> voidPurchase(
            @PathVariable Long id,
            @Valid @RequestBody VoidPurchaseRequest request) {
        PurchaseResponse response = purchaseService.voidPurchase(id, request);
        return ResponseEntity.ok(response);
    }
}
