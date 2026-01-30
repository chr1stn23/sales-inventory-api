package com.christn.salesinventoryapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sales",
        indexes = {
                // Optimizar búsquedas de ventas por cliente
                @Index(name = "idx_sale_customer", columnList = "customer_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Sale extends BaseEntity {
    @Column(nullable = false)
    private LocalDateTime saleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status = SaleStatus.ACTIVE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleDetail> details = new ArrayList<>();

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by", length = 255)
    private String voidedBy;

    @Column(name = "voided_by_user_id")
    private Long voidedByUserId;

    @Column(name = "void_reason", length = 255)
    private String voidReason;
}
