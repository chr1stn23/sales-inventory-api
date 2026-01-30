package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    List<Sale> findAllByDeletedFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
               UPDATE Sale s
               SET s.status = :status,
                   s.voidedAt = :voidedAt,
                   s.voidedBy = :voidedBy,
                   s.voidedByUserId = :voidedByUserId,
                   s.voidReason = :voidReason
               WHERE s.id = :id
            """)
    int markVoided(
            @Param("id") Long id,
            @Param("status") SaleStatus status,
            @Param("voidedAt") LocalDateTime voidedAt,
            @Param("voidedBy") String voidedBy,
            @Param("voidedByUserId") Long voidedByUserId,
            @Param("voidReason") String voidReason
    );

    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findBasicById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "details", "details.product"})
    Optional<Sale> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

}
