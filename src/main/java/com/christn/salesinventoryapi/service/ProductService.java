package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.ProductRequest;
import com.christn.salesinventoryapi.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    List<ProductResponse> findAll();

    List<ProductResponse> findAllByCategoryId(Long categoryId);

    ProductResponse findById(Long id);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
