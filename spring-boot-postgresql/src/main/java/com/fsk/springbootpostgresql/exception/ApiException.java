package com.fsk.springbootpostgresql.exception;

public abstract class ApiException extends RuntimeException {
    protected ApiException(String message) {
        super(message);
    }

    public abstract String getCode();
}
