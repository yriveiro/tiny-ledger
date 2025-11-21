package com.acme.api.ledger.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.acme.api.ledger.exception.LedgerNotFoundException;
import com.acme.api.ledger.exception.TransactionAlreadyExistsException;
import com.acme.api.ledger.model.LedgerKey;
import com.acme.api.ledger.model.Transaction;
import com.fasterxml.uuid.Generators;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryLedgerRepository {
    /**
     * Initially I was using a ConcurrentSkipListMap for the store, but is not
     * thread safe. ConcurrentSkipListMap is thread safe and maintains order,
     * consumes more memory but not a problem here.
     *
     * https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html
     */
    private final ConcurrentHashMap<LedgerKey, ConcurrentSkipListMap<UUID, Transaction>> store = new ConcurrentHashMap<>();

    public boolean save(LedgerKey key) {
        if (store.containsKey(key)) {
            Log.warnf("Ledger already exists: %s", key);

            return false;
        }

        ConcurrentSkipListMap<UUID, Transaction> transactions = new ConcurrentSkipListMap<>();

        store.put(key, transactions);

        Log.debugf("Store: %s", store.toString());

        return true;
    }

    public boolean exists(LedgerKey key) {
        return store.containsKey(key);
    }

    public Map.Entry<UUID, Transaction> add(LedgerKey key, Transaction transaction) {
        ConcurrentSkipListMap<UUID, Transaction> transactions = getTransactionsOrThrow(key);

        UUID trxId = Generators.timeBasedEpochGenerator().generate();

        if (store.get(key).containsKey(trxId)) {
            Log.warnf("Transaction already exists in ledger %s with transaction id %s", key, trxId);

            throw new TransactionAlreadyExistsException("Ledger %s -> transaction %s".formatted(key, trxId));
        }

        transactions.put(trxId, transaction);

        Log.infof("Transaction added to service %s: transaction id=%s, model=%s", key, trxId, transaction.operation());

        return Map.entry(trxId, transaction);
    }

    public List<Map.Entry<UUID, Transaction>> history(LedgerKey key, Instant start, Instant end) {
        ConcurrentSkipListMap<UUID, Transaction> transactions = getTransactionsOrThrow(key);
        List<UUID> _uuids = range(start, end);

        return transactions
                .subMap(_uuids.get(0), true, _uuids.get(1), true)
                .entrySet()
                .stream()
                .toList();
    }

    // LLM gen. function, bitwise operations are archived in brain's tape storage 😅
    private List<UUID> range(Instant from, Instant to) {
        long epochFrom = from.toEpochMilli();
        long epochTo = to.toEpochMilli();

        long v7VersionBits = 0x7L << 12;

        long startMsb = (epochFrom << 16) | v7VersionBits;
        long startLsb = 0x8000_0000_0000_0000L;

        long endMsb = (epochTo << 16) | v7VersionBits | 0x0FFF; // max 12b subms
        long endLsb = 0xBFFF_FFFF_FFFF_FFFFL; // max for variant=2 (10xx...)

        return List.of(new UUID(startMsb, startLsb), new UUID(endMsb, endLsb));
    }

    public BigDecimal balance(LedgerKey key) {
        ConcurrentSkipListMap<UUID, Transaction> transactions = getTransactionsOrThrow(key);

        // This operations sucks in big ledgers -> O(n)
        return transactions.values().stream()
                .map(tx -> tx.operation() == Transaction.Operation.DEPOSIT ? tx.amount() : tx.amount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ConcurrentSkipListMap<UUID, Transaction> getTransactionsOrThrow(LedgerKey key) {
        ConcurrentSkipListMap<UUID, Transaction> transactions = store.get(key);

        if (transactions == null) {
            throw new LedgerNotFoundException(String.format("Ledger not found: %s", key));
        }

        return transactions;
    }
}
