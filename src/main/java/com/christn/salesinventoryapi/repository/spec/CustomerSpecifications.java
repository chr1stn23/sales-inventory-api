package com.christn.salesinventoryapi.repository.spec;

import com.christn.salesinventoryapi.model.Customer;
import org.springframework.data.jpa.domain.Specification;

public class CustomerSpecifications {

    public static Specification<Customer> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Customer> emailContains(String term) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + term.toLowerCase() + "%");
    }

    public static Specification<Customer> nameContains(String term) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("fullName")), "%" + term.toLowerCase() + "%");
    }
}
