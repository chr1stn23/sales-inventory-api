package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CreatePurchaseRequest;
import com.christn.salesinventoryapi.dto.request.PostPurchaseRequest;
import com.christn.salesinventoryapi.dto.request.VoidPurchaseRequest;
import com.christn.salesinventoryapi.dto.response.PurchaseResponse;

public interface PurchaseService {

    PurchaseResponse createDraft(CreatePurchaseRequest request);

    PurchaseResponse postPurchase(Long purchaseId, PostPurchaseRequest request);

    PurchaseResponse getById(Long id);

    PurchaseResponse voidPurchase(Long purchaseId, VoidPurchaseRequest request);
}
