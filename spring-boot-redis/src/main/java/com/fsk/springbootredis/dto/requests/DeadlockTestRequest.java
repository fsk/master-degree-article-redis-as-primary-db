package com.fsk.springbootredis.dto.requests;

import java.util.UUID;

public record DeadlockTestRequest(UUID firstProductId, UUID secondProductId, Long delayMs) {
}
