package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.ProductResponse;
import com.christn.salesinventoryapi.model.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product prod) {
        return new ProductResponse(
                prod.getId(),
                prod.getName(),
                prod.getDescription(),
                prod.getPrice(),
                prod.getStock(),
                CategoryMapper.toResponse(prod.getCategory())
        );
    }
}
