package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.CategoryResponse;
import com.christn.salesinventoryapi.model.Category;

public class CategoryMapper {

    public static CategoryResponse toResponse(Category cat) {
        return new CategoryResponse(
                cat.getId(),
                cat.getName(),
                cat.getDescription()
        );
    }
}
