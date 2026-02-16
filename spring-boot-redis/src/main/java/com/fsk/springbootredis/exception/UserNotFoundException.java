package com.fsk.springbootredis.exception;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException() {
        super("User not found");
    }

    @Override
    public String getCode() {
        return "USER_NOT_FOUND";
    }
}
