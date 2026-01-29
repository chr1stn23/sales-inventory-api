package com.christn.salesinventoryapi.repository.spec;

import com.christn.salesinventoryapi.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecifications {

    public static Specification<Product> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Product> categoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> query(String q) {
        return (root, query, cb) -> {
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<Product> minStock(Integer minStock) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("stock"), minStock);
    }

    public static Specification<Product> maxStock(Integer maxStock) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("stock"), maxStock);
    }

    public static Specification<Product> minPrice(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> maxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
