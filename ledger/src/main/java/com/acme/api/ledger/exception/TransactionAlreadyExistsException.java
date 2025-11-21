package com.acme.api.ledger.exception;

public class TransactionAlreadyExistsException extends RuntimeException {

    public TransactionAlreadyExistsException(String message) {
        super(message);
    }
}
