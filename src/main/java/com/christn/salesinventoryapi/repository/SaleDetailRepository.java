package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.SaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleDetailRepository extends JpaRepository<SaleDetail, Long> {

    @Query("""
                SELECT d.product.id, d.quantity
                FROM SaleDetail d
                WHERE d.sale.id = :saleId
            """)
    List<Object[]> findProductIdAndQtyBySaleId(@Param("saleId") Long saleId);
}
