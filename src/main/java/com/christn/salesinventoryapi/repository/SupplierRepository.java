package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
