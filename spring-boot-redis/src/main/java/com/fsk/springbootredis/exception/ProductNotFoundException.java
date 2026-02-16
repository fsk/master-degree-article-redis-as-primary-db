package com.fsk.springbootredis.exception;

public class ProductNotFoundException extends ApiException {
    public ProductNotFoundException() {
        super("Product not found");
    }

    @Override
    public String getCode() {
        return "PRODUCT_NOT_FOUND";
    }
}
