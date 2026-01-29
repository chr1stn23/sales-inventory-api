package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    List<Sale> findAllByDeletedFalse();

    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

}
