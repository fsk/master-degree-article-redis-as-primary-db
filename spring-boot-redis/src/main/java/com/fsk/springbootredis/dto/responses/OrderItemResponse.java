package com.fsk.springbootredis.dto.responses;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
