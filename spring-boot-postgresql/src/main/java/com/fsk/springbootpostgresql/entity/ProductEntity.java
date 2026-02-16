package com.fsk.springbootpostgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import com.fsk.springbootpostgresql.exception.InsufficientStockException;
import com.fsk.springbootpostgresql.exception.StockBelowZeroException;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    public void applyStockDelta(int delta) {
        int updated = stockQuantity + delta;
        if (updated < 0) {
            throw new StockBelowZeroException();
        }
        this.stockQuantity = updated;
    }

    public void decreaseStock(int quantity) {
        int remaining = stockQuantity - quantity;
        if (remaining < 0) {
            throw new InsufficientStockException();
        }
        this.stockQuantity = remaining;
    }
}
