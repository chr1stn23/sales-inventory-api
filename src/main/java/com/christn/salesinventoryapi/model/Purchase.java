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
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
public class Purchase extends BaseEntity {

    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status = PurchaseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private PurchaseDocumentType documentType = PurchaseDocumentType.INVOICE;

    @Column(name = "document_number", length = 60)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String notes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "posted_by_user_id")
    private Long postedByUserId;

    @JoinColumn(name = "voided_by_user_id")
    private Long voidedByUserId;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();
}
