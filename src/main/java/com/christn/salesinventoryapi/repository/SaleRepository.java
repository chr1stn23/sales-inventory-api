package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByDeletedFalse();

}
