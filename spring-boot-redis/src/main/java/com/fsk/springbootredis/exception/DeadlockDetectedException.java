package com.fsk.springbootredis.exception;

public class DeadlockDetectedException extends ApiException {
    public DeadlockDetectedException() {
        super("Deadlock detected");
    }

    @Override
    public String getCode() {
        return "DEADLOCK_DETECTED";
    }
}
