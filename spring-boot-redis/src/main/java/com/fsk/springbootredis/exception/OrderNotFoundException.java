package com.fsk.springbootredis.exception;

public class OrderNotFoundException extends ApiException {
    public OrderNotFoundException() {
        super("Order not found");
    }

    @Override
    public String getCode() {
        return "ORDER_NOT_FOUND";
    }
}
