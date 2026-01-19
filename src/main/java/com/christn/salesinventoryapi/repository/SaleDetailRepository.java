package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.SaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleDetailRepository extends JpaRepository<SaleDetail, Long> {
}
