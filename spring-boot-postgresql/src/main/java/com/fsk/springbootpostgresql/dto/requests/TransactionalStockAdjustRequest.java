package com.fsk.springbootpostgresql.dto.requests;

import java.util.UUID;

public record TransactionalStockAdjustRequest(UUID productId, Integer delta, Long delayMs) {
}
