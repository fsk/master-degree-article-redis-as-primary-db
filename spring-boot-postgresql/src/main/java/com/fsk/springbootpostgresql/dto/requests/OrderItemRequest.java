package com.fsk.springbootpostgresql.dto.requests;

import java.util.UUID;

public record OrderItemRequest(UUID productId, Integer quantity) {
}
