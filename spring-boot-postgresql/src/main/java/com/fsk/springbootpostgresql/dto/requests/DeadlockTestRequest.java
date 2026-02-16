package com.fsk.springbootpostgresql.dto.requests;

import java.util.UUID;

public record DeadlockTestRequest(UUID firstProductId, UUID secondProductId, Long delayMs) {
}
