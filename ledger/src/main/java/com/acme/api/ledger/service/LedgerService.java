package com.acme.api.ledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.acme.api.ledger.exception.InvalidHistoryQueryException;
import com.acme.api.ledger.model.Currency;
import com.acme.api.ledger.model.LedgerKey;
import com.acme.api.ledger.model.Transaction;
import com.acme.api.ledger.repository.InMemoryLedgerRepository;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

@ApplicationScoped
public class LedgerService {
    @Inject
    InMemoryLedgerRepository store;

    public Boolean create(LedgerKey key) {
        if (store.exists(key)) {
            Log.warnf("Ledger already exists: %s, conflict", key);

            return false;
        }

        boolean _ = store.save(key);

        return true;
    }

    public BigDecimal balance(LedgerKey key) {
        return store.balance(key);
    }

    public List<Map.Entry<UUID, Transaction>> history(LedgerKey key, Instant start, Instant end) {
        if (start.isAfter(end)) {
            throw new InvalidHistoryQueryException(start, end);
        }

        return store.history(key, start, end);
    }

    public boolean exists(LedgerKey key) {
        return store.exists(key);
    }

    public Map.Entry<UUID, Transaction> commit(
            LedgerKey key,
            Transaction.Operation operation,
            @Digits(integer = 15, fraction = 2) @Positive(message = "Amount must be greater than zero") BigDecimal amount,
            String description,
            String reference,
            Currency currency) {
        Transaction trx = new Transaction(
                operation,
                amount,
                description,
                reference,
                currency);

        return store.add(key, trx);
    }
}
