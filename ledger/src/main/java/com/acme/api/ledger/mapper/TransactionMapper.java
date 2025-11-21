package com.acme.api.ledger.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.acme.api.ledger.apispec.dto.Transaction;

import jakarta.enterprise.context.ApplicationScoped;

// Mapper to convert internal Transaction model to API Transaction DTO,
// basically zips the UUID with the Transaction

@ApplicationScoped
public class TransactionMapper {
    public Transaction toDto(UUID id, com.acme.api.ledger.model.Transaction transaction) {
        return new Transaction()
                .id(id)
                .type(Transaction.TypeEnum.fromValue(transaction.operation().toString()))
                .value(transaction.amount())
                .description(transaction.description())
                .reference(transaction.reference());
    }

    public List<Transaction> toDtoList(List<Map.Entry<UUID, com.acme.api.ledger.model.Transaction>> transactions) {
        return transactions.stream()
                .map(entry -> toDto(entry.getKey(), entry.getValue()))
                .toList();
    }
}
