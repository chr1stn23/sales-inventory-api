package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndDeletedFalse(Long id);

    boolean existsByEmailAndDeletedFalse(String email);
}
