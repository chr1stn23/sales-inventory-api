package com.christn.salesinventoryapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
public class Sale extends BaseEntity {
    @Column(nullable = false)
    private LocalDateTime saleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status = SaleStatus.ACTIVE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by_user_id")
    private Long voidedByUserId;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "posted_by_user_id")
    private Long postedByUserId;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "completed_by_user_id")
    private Long completedByUserId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleDetail> details = new ArrayList<>();
}
