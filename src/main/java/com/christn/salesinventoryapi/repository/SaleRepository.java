package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Sale;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT DISTINCT s FROM Sale s
                LEFT JOIN FETCH s.details d
                LEFT JOIN FETCH d.product p
                LEFT JOIN FETCH s.customer c
                WHERE s.id = :id
            """)
    Optional<Sale> findByIdWithDetailsForUpdate(@Param("id") Long id);

    @Query("""
                SELECT DISTINCT s FROM Sale s
                LEFT JOIN FETCH s.details d
                LEFT JOIN FETCH d.product p
                LEFT JOIN FETCH s.customer c
                WHERE s.id = :id
            """)
    Optional<Sale> findByIdWithDetails(@Param("id") Long id);


    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

}
