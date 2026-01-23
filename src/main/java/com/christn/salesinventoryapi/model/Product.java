package com.christn.salesinventoryapi.model;

import com.christn.salesinventoryapi.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "products",
        indexes = {
                // Optimizar búsquedas de productos por categoria
                @Index(name = "idx_product_category", columnList = "category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public void validateStock(int quantity) {
        if (stock < quantity) {
            throw new InsufficientStockException(getName());
        }
    }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }
}
