package com.fsk.springbootredis.dto.requests;

import java.math.BigDecimal;

public record CreateProductRequest(String name, BigDecimal price, Integer stockQuantity) {
}
