package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.ProductBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    @Query("""
            SELECT b FROM ProductBatch b
            JOIN FETCH b.product
            JOIN FETCH b.purchaseItem pi
            WHERE pi.purchase.id = :purchaseId
            """)
    List<ProductBatch> findBatchesByPurchaseId(@Param("purchaseId") Long purchaseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT b FROM ProductBatch b
                WHERE b.product.id IN :productIds
                  AND b.qtyAvailable > 0
                ORDER BY
                  b.expiresAt ASC NULLS LAST,
                  b.receivedAt ASC,
                  b.id ASC
            """)
    List<ProductBatch> findAvailableBatchesForUpdate(@Param("productIds") List<Long> productIds);

}
