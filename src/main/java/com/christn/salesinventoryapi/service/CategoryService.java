package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CategoryRequest;
import com.christn.salesinventoryapi.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    List<CategoryResponse> findAll();

    CategoryResponse findById(Long id);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
