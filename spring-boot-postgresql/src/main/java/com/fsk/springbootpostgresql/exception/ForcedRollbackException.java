package com.fsk.springbootpostgresql.exception;

public class ForcedRollbackException extends ApiException {
    public ForcedRollbackException() {
        super("Forced rollback for testing");
    }

    @Override
    public String getCode() {
        return "FORCED_ROLLBACK";
    }
}
