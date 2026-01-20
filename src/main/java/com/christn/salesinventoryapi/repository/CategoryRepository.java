package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByDeletedFalse();

    Optional<Category> findByIdAndDeletedFalse(Long id);

    boolean existsByNameAndDeletedFalse(String name);
}
