package com.fsk.springbootredis.entity;

import com.fsk.springbootredis.exception.InsufficientStockException;
import com.fsk.springbootredis.exception.StockBelowZeroException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@RedisHash("products")
public class ProductEntity implements Serializable {
    @Id
    private UUID id;
    private String name;
    private BigDecimal price;
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
