package com.fsk.springbootredis.entity;

import com.fsk.springbootredis.exception.InvalidOrderQuantityException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@RedisHash("orders")
public class OrderEntity implements Serializable {
    @Id
    private UUID id;
    private UUID userId;
    private Instant createdAt;
    private OrderStatus status;
    private List<OrderItemEntity> items = new ArrayList<>();

    public void addItem(ProductEntity product, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidOrderQuantityException();
        }
        product.decreaseStock(quantity);

        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setProductId(product.getId());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(product.getPrice());
        items.add(orderItem);
    }
}
