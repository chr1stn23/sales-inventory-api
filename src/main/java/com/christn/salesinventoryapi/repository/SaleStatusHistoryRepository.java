package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.SaleStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleStatusHistoryRepository extends JpaRepository<SaleStatusHistory, Long> {

    List<SaleStatusHistory> findBySaleIdOrderByChangedAtDesc(Long saleId);
}
