package com.acme.api.ledger.model;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LedgerKey(
        @NotNull(message = "Ledger id cannot be null") UUID id,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code") Currency currency) {

    @Override
    public String toString() {
        return String.format("LedgerKey(%s, %s)", id, currency);
    }
}
