package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.SaleBatchAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleBatchAllocationRepository extends JpaRepository<SaleBatchAllocation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT a FROM SaleBatchAllocation a
                JOIN FETCH a.productBatch b
                JOIN FETCH b.product p
                WHERE a.saleDetail.sale.id = :saleId
            """)
    List<SaleBatchAllocation> findAllBySaleIdForUpdate(@Param("saleId") Long saleId);

}
