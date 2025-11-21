package com.acme.api.ledger.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record Currency(
        @NotNull(message = "Currency code cannot be null") @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code (3 uppercase letters)") String code) {

    public static Currency of(String code) {
        return new Currency(code);
    }

    public String value() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
