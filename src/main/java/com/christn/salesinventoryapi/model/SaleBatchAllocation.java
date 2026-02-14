package com.christn.salesinventoryapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sale_batch_allocations")
@Getter
@Setter
@NoArgsConstructor
public class SaleBatchAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_detail_id", nullable = false)
    private SaleDetail saleDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_batch_id", nullable = false)
    private ProductBatch productBatch;

    @Column(nullable = false)
    private Integer quantity;
}
