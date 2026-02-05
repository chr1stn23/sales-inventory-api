package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @EntityGraph(attributePaths = {"customer", "details", "details.product"})
    Optional<Sale> findWithDetailsById(Long id);

    @Query("SELECT s FROM Sale s JOIN FETCH s.customer LEFT JOIN FETCH s.details d WHERE s.id = :id")
    Optional<Sale> findWithDetailsNoProductsById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

}
