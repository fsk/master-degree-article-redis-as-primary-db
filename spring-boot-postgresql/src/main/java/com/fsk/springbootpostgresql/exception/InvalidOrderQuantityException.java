package com.fsk.springbootpostgresql.exception;

public class InvalidOrderQuantityException extends ApiException {
    public InvalidOrderQuantityException() {
        super("Order item quantity must be greater than zero");
    }

    @Override
    public String getCode() {
        return "ORDER_ITEM_QUANTITY_INVALID";
    }
}
