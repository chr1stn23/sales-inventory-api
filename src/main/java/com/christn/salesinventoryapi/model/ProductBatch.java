package com.christn.salesinventoryapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_batches")
@Getter
@Setter
public class ProductBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id")
    private PurchaseItem purchaseItem;

    @Column(name = "batch_code", length = 80)
    private String batchCode;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "qty_initial", nullable = false)
    private Integer qtyInitial;

    @Column(name = "qty_available", nullable = false)
    private Integer qtyAvailable;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;
}
