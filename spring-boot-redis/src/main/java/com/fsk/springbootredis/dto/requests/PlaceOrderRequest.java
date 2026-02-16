package com.fsk.springbootredis.dto.requests;

import com.fsk.springbootredis.exception.EmptyOrderItemsException;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(UUID userId, List<OrderItemRequest> items) {
    public void validateItems() {
        if (items == null || items.isEmpty()) {
            throw new EmptyOrderItemsException();
        }
    }
}
