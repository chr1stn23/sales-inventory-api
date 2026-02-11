package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Purchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    // Para lock + usar en post/void (SIN batches)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT DISTINCT p FROM Purchase p
                LEFT JOIN FETCH p.supplier
                LEFT JOIN FETCH p.items i
                LEFT JOIN FETCH i.product
                WHERE p.id = :id
            """)
    Optional<Purchase> findByIdWithAllForUpdate(@Param("id") Long id);

    // Para DETAIL base (SIN batches)
    @Query("""
                SELECT DISTINCT p FROM Purchase p
                LEFT JOIN FETCH p.supplier
                LEFT JOIN FETCH p.items i
                LEFT JOIN FETCH i.product
                WHERE p.id = :id
            """)
    Optional<Purchase> findDetailBaseById(@Param("id") Long id);
}
