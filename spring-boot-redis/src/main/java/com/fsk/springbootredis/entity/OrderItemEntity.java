package com.fsk.springbootredis.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity implements Serializable {
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
