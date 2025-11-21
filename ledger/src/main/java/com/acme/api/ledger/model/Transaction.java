package com.acme.api.ledger.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record Transaction(

        @NotNull(message = "Operation model cannot be null") Operation operation,

        @NotNull(message = "Amount cannot be null") @Positive(message = "Amount must be greater than zero") @Digits(integer = 15, fraction = 2, message = "Amount must have at most 15 digits and 2 decimal places") BigDecimal amount,

        @NotBlank(message = "Description cannot be null or blank") @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters") String description,

        @NotBlank(message = "Reference cannot be null or blank") @Size(min = 1, max = 100, message = "Reference must be between 1 and 100 characters") String reference,

        @NotNull(message = "Currency cannot be null") Currency currency) {

    public static enum Operation {
        DEPOSIT,
        WITHDRAWAL
    }
}
