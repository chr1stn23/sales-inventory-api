package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedFalse(Long id);

    List<Product> findAllByDeletedFalse();

    List<Product> findAllByCategoryIdAndDeletedFalse(Long categoryId);

    boolean existsByNameAndDeletedFalse(String name);
}
