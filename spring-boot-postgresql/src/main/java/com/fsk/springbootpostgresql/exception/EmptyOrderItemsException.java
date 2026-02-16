package com.fsk.springbootpostgresql.exception;

public class EmptyOrderItemsException extends ApiException {
    public EmptyOrderItemsException() {
        super("Order items cannot be empty");
    }

    @Override
    public String getCode() {
        return "ORDER_ITEMS_EMPTY";
    }
}
