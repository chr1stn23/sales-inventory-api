package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
