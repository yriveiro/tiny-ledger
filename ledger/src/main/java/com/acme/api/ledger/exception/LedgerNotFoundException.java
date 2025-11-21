package com.acme.api.ledger.exception;

public class LedgerNotFoundException extends RuntimeException {

    public LedgerNotFoundException(String message) {
        super(message);
    }
}
