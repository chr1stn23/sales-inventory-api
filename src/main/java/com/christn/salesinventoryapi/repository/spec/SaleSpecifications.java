package com.christn.salesinventoryapi.repository.spec;

import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleSpecifications {

    public static Specification<Sale> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Sale> customerId(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Sale> from(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("saleDate"), from);
    }

    public static Specification<Sale> to(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("saleDate"), to);
    }

    public static Specification<Sale> minTotal(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
    }

    public static Specification<Sale> maxTotal(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("totalAmount"), max);
    }

    public static Specification<Sale> status(SaleStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
