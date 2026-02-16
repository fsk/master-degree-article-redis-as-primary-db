package com.fsk.springbootpostgresql.dto.requests;

import java.math.BigDecimal;

public record CreateProductRequest(String name, BigDecimal price, Integer stockQuantity) {
}
