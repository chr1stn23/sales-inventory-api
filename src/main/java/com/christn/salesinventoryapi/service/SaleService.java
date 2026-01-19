package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.SaleResponse;

import java.util.List;

public interface SaleService {

    SaleResponse create(SaleRequest request);

    List<SaleResponse> findAll();
}
