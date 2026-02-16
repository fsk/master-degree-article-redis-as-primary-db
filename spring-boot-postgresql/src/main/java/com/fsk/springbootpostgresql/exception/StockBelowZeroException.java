package com.fsk.springbootpostgresql.exception;

public class StockBelowZeroException extends ApiException {
    public StockBelowZeroException() {
        super("Stock cannot be below zero");
    }

    @Override
    public String getCode() {
        return "STOCK_BELOW_ZERO";
    }
}
