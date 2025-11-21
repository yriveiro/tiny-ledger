# Ledger API Documentation

This document describes the Ledger API as part of the Tiny Ledger project, which
provides endpoints for managing user ledgers, including creating ledgers, depositing
and withdrawing funds, checking balances, and viewing transaction history.

# API

This resource allows the system perform the following operations:

- Create a new ledger.
- Retrieve the current balance of the account.
- Deposit funds into the ledger.
- Withdraw funds from the ledger.
- Retrieve the transaction history for the ledger.

> [!NOTE]
> Initially I was planning to have an Account API and a Ledger API. However,
> I decided to not do it and simplify the implementation and focus on concepts.
> I was running out of time.

## How to Use

### Running the API

Makefile with different targets exists: :

- **Development mode (JVM)**: `make dev`
- **JVM flavour**: `make run-jvm`
- **Native flavour**: `make run-native`

### Testing

- Run tests: `make test`
- Test the API with the provided script: `./scripts/test.sh`

### Build Targets

- run `make` and the default target will show the available targets.

## Assumptions

- No customised page errors for Quarkus. Any jakarta validation errors will be
  returned as is.
- Negative balances are allowed.
- Allowed only two type of operations with the ledger, DEPOSIT and WITHDRAWAL.
- The balance endpoint returns the current balance doing a reduce operation across
all transactions. This clearly would not scale for a production, but for the
exercise simplifies the implementation of the storage.
- History period is hardcoded to current day.

### Storage

Storage will be an in-memory data structure for simplicity as requested.

Basically the storage is a ```ConcurrentHashMap<LedgerKey, ConcurrentSkipListMap<UUID, Transaction>>```
where UUID is a UUIDv7 to ensure chronological order of transactions.

### Authentication and Authorization

Not implemented as requested in the guidelines.

It could be implemented with Apache Keycloak, Vault or other system.

### Observability

Logging is basic and monitoring are not included, as requested in the guidelines

Following OpenTelemetry standards and instrumenting the code with tracing and metrics
would be the recommended approach for production systems.

### CDC

- No events. This is important as the correct way to implement the history should
be send events to an event bus and allow to keep the CDC in a more appropriate storage
to ensure longer retention and not bloat the operational database.

### Other considerations

- Error handling is basic and may need to be expanded for production use.

# Architecture

```mermaid
graph TB
    subgraph Client["Client"]
        UI["User Interface"]
    end

    subgraph API["API"]
        direction LR
        CreateAPI["POST /ledgers"]
        AddCurrencyAPI["POST /ledgers/{id}/currencies"]
        DepositAPI["POST /ledgers/{id}/transactions/{currency}/deposit"]
        WithdrawalAPI["POST /ledgers/{id}/transactions/{currency}/withdrawal"]
        BalanceAPI["GET /ledgers/{id}/balances/{currency}"]
        HistoryAPI["GET /ledgers/{id}/transactions/{currency}"]
    end

    subgraph Service["Services"]
        LedgerService["Ledger Service"]
    end

    subgraph DB["Database"]
        LedgerDB["LedgerRepository"]
    end

    UI --> CreateAPI
    UI --> AddCurrencyAPI
    UI --> DepositAPI
    UI --> WithdrawalAPI
    UI --> BalanceAPI
    UI --> HistoryAPI

    CreateAPI --> LedgerService
    AddCurrencyAPI --> LedgerService
    DepositAPI --> LedgerService
    WithdrawalAPI --> LedgerService
    BalanceAPI --> LedgerService
    HistoryAPI --> LedgerService

    LedgerService --> LedgerDB

    style Client fill:#4a90e2,stroke:#2e5c8a,color:#fff
    style API fill:#7b68ee,stroke:#5a4ba5,color:#fff
    style Service fill:#50c878,stroke:#2d7a4a,color:#fff
    style DB fill:#e85d75,stroke:#a53a4a,color:#fff
```

# Storage Schema

```mermaid
graph LR
    HashMap["HashMap<br/>Key: UUID + Currency<br/>Value: Array[UUIDv7]"]

    Entry1["Entry 1<br/>UUID-1 + USD"]
    Array1["Array[UUIDv7]"]
    T1["UUIDv7-001<br/>DEPOSIT 500.00<br/>Salary / PAY-001"]
    T2["UUIDv7-002<br/>WITHDRAWAL 150.00<br/>Bill / INV-001"]

    Entry2["Entry 2<br/>UUID-2 + EUR"]
    Array2["Array[UUIDv7]"]
    T3["UUIDv7-001<br/>DEPOSIT 1000.00<br/>Transfer / WIRE-001"]

    Entry3["Entry N<br/>UUID-N + XXX"]

    Entry4["Entry 4<br/>UUID-1 + USD"]
    Array4["Array[UUIDv7]"]
    T4["UUIDv7-001<br/>DEPOSIT 658.90<br/>Transfer / USE-WIRE-001"]

    HashMap --> Entry1
    Entry1 --> Array1
    Array1 --> T1
    Array1 --> T2

    HashMap --> Entry2
    Entry2 --> Array2
    Array2 --> T3

    HashMap --> Entry3

    HashMap --> Entry4
    Entry4 --> Array4
    Array4 --> T4

    subgraph Perf["Performance"]
        P1["O(1) ledger lookup by UUID+Currency"]
        P2["Chronological order via UUIDv7"]
        P3["In-memory storage"]
    end

    style HashMap fill:#ff6b6b,stroke:#cc0000,color:#fff,font-weight:bold
    style Entry1 fill:#ffa500,stroke:#cc8400,color:#fff
    style Entry2 fill:#ffa500,stroke:#cc8400,color:#fff
    style Entry3 fill:#ffa500,stroke:#cc8400,color:#fff
    style Entry4 fill:#ffa500,stroke:#cc8400,color:#fff
    style Array1 fill:#50c878,stroke:#2d7a4a,color:#fff
    style Array2 fill:#50c878,stroke:#2d7a4a,color:#fff
    style Array4 fill:#50c878,stroke:#2d7a4a,color:#fff
    style T1 fill:#4a90e2,stroke:#2e5c8a,color:#fff
    style T2 fill:#4a90e2,stroke:#2e5c8a,color:#fff
    style T3 fill:#4a90e2,stroke:#2e5c8a,color:#fff
    style T4 fill:#4a90e2,stroke:#2e5c8a,color:#fff
    style Perf fill:#34495e,stroke:#2c3e50,color:#fff
```
