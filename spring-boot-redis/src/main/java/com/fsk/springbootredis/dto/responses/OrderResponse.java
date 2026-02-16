package com.fsk.springbootredis.dto.responses;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private UUID userId;
    private Instant createdAt;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
}
