package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
                WHERE p.sale.id = :saleId AND p.status = 'POSTED'
            """)
    BigDecimal sumPostedBySaleId(@Param("saleId") Long saleId);
}
