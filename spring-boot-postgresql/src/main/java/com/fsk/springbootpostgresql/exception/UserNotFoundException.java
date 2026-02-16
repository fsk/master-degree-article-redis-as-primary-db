package com.fsk.springbootpostgresql.exception;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException() {
        super("User not found");
    }

    @Override
    public String getCode() {
        return "USER_NOT_FOUND";
    }
}
