package com.acme.api.ledger.exception;

import java.time.Instant;

public class InvalidHistoryQueryException extends RuntimeException {

    public InvalidHistoryQueryException(Instant start, Instant end) {
        super(String.format("Invalid history query: start time (%s) cannot be after end time (%s)",
                start, end));
    }
}
