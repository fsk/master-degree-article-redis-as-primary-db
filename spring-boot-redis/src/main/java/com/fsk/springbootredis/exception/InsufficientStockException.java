package com.fsk.springbootredis.exception;

public class InsufficientStockException extends ApiException {
    public InsufficientStockException() {
        super("Insufficient stock");
    }

    @Override
    public String getCode() {
        return "INSUFFICIENT_STOCK";
    }
}
