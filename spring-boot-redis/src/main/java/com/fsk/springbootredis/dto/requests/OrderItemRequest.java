package com.fsk.springbootredis.dto.requests;

import java.util.UUID;

public record OrderItemRequest(UUID productId, Integer quantity) {
}
