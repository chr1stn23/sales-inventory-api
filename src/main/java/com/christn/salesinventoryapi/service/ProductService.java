package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.ProductRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    List<ProductResponse> findAll();

    List<ProductResponse> findAllByCategoryId(Long categoryId);

    ProductResponse findById(Long id);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    void restore(Long id);

    PageResponse<ProductResponse> search(
            String query,
            Long categoryId,
            Integer minStock,
            Integer maxStock,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );
}
