package com.christn.salesinventoryapi.repository.spec;

import com.christn.salesinventoryapi.model.Role;
import com.christn.salesinventoryapi.model.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

public class UserSpecifications {

    public static Specification<User> isEnabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) return cb.conjunction();
            return cb.equal(root.get("enabled"), enabled);
        };
    }

    public static Specification<User> emailContains(String term) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + term.toLowerCase() + "%");
    }

    public static Specification<User> createdAtFrom(LocalDateTime from) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<User> createdAtTo(LocalDateTime to) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<User> hasAnyRole(Set<Role> roles) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<User, Role> rolesJoin = root.join("roles");
            return rolesJoin.in(roles);
        };
    }
}
