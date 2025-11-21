package com.acme.api.ledger.resource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.acme.api.ledger.apispec.dto.AddCurrencyRequest;
import com.acme.api.ledger.apispec.dto.BalanceResponse;
import com.acme.api.ledger.apispec.dto.CreateLedgerRequest;
import com.acme.api.ledger.apispec.dto.CreateLedgerResponse;
import com.acme.api.ledger.apispec.dto.CurrencyLedgerResponse;
import com.acme.api.ledger.apispec.dto.TransactionRequest;
import com.acme.api.ledger.apispec.dto.TransactionResponse;
import com.acme.api.ledger.apispec.dto.TransactionsResponse;
import com.acme.api.ledger.apispec.dto.ValidationResponse;
import com.acme.api.ledger.exception.TransactionAlreadyExistsException;
import com.acme.api.ledger.mapper.TransactionMapper;
import com.acme.api.ledger.model.Currency;
import com.acme.api.ledger.model.LedgerKey;
import com.acme.api.ledger.model.Transaction;
import com.acme.api.ledger.service.LedgerService;

import io.quarkus.logging.Log;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/ledgers")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LedgerResource {

    @Inject
    LedgerService svc;

    @Inject
    TransactionMapper mapper;

    final String CURRENCY_REGEX = "^[A-Z]{3}$";
    final String CURRENCY_MESSAGE = "Currency must be a valid ISO 4217 code (3 uppercase letters)";
    final String JSON_BODY_MESSAGE = "JSON body is required";

    @POST
    public Response ledgers(@Valid CreateLedgerRequest request) {
        LedgerKey key = switch (request) {
            case null -> new LedgerKey(UUID.randomUUID(), Currency.of("EUR"));
            case CreateLedgerRequest req -> new LedgerKey(req.getId(), Currency.of(req.getCurrency()));
        };

        try {

            Log.infof("Creating service %s", key);

            Boolean ok = svc.create(key);

            if (!ok) {
                Log.warnf("Ledger already exists: %s", key);

                return Response.status(Response.Status.CONFLICT).build();
            }

            CreateLedgerResponse dto = new CreateLedgerResponse();

            dto.setId(key.id());
            dto.setCurrency(key.currency().value());

            return Response.status(Response.Status.CREATED).entity(dto).build();
        } catch (Exception e) {
            Log.errorf("Error creating service %s: %s", key, e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Log.infof("Finished processing create service %s", key);
        }
    }

    @POST
    @Path("/{id}/currencies")
    public Response currencies(@PathParam("id") @NotNull UUID id, @Valid AddCurrencyRequest request) {
        Currency currency = Currency.of(request.getCurrency());

        LedgerKey key = new LedgerKey(id, currency);

        try {

            Log.infof("Add currency %s to service %s", currency, key);

            Boolean ok = svc.create(key);

            if (!ok) {
                Log.warnf("Ledger already exists: %s", key);

                return Response.status(Response.Status.CONFLICT).build();
            }

            CurrencyLedgerResponse dto = new CurrencyLedgerResponse();

            dto.setCurrency(currency.value());
            dto.setBalance(BigDecimal.ZERO);

            return Response.status(Response.Status.CREATED).entity(dto).build();
        } catch (Exception e) {
            Log.errorf("Error adding currency %s to service %s: %s", currency, key, e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Log.infof("Finished adding currency %s to service %s", currency, key);
        }
    }

    @GET
    @Path("/{id}/balance/{currency}")
    public Response balance(
            @PathParam("id") @NotNull UUID id,
            @PathParam("currency") @Pattern(regexp = CURRENCY_REGEX, message = CURRENCY_MESSAGE) @NotBlank String currency) {

        // @Pattern only works with Strings, Currency is a custom model, ergo I
        // would need to create a custom validator. Not worthy for this assessment.
        Currency _currency = Currency.of(currency);

        LedgerKey key = new LedgerKey(id, _currency);

        Log.infof("Retrieving balance for currency %s and service  %s", currency, key);

        try {

            if (!svc.exists(key)) {
                Log.warnf("Ledger not found: %s", key);

                return Response.status(Response.Status.NOT_FOUND).build();
            }

            BalanceResponse dto = new BalanceResponse();

            dto.setId(id);
            dto.setCurrency(currency);
            dto.setBalance(svc.balance(key));

            return Response.ok().entity(dto).build();
        } catch (Exception e) {
            Log.errorf("Error checking existence of service %s: %s", id, e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{id}/transactions/{currency}")
    public Response history(
            @PathParam("id") @NotNull UUID id,
            @PathParam("currency") @Pattern(regexp = CURRENCY_REGEX, message = CURRENCY_MESSAGE) @NotBlank String currency) {

        // @Pattern only works with Strings, Currency is a custom model, ergo I
        // would need to create a custom validator. Not worthy for this assessment.
        Currency _currency = Currency.of(currency);

        LedgerKey key = new LedgerKey(id, _currency);

        Log.infof("Retrieving last 24h history for service %s with currency %s", key, currency);

        try {

            if (!svc.exists(key)) {
                Log.warnf("Ledger not found: %s", key);

                return Response.status(Response.Status.NOT_FOUND).build();
            }

            Instant start = Instant.now()
                    .atZone(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();

            Instant end = start.plus(Duration.ofDays(7));

            TransactionsResponse dto = new TransactionsResponse();

            dto.setId(id);
            dto.setCurrency(currency);

            List<Map.Entry<UUID, com.acme.api.ledger.model.Transaction>> history = svc.history(key, start, end);

            dto.setTransactions(mapper.toDtoList(history));

            return Response.ok().entity(dto).build();
        } catch (Exception e) {
            Log.errorf("Error checking history for service %s: %s", id, e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/{id}/transactions/{currency}/deposit")
    public Response deposit(
            @PathParam("id") @NotNull UUID id,
            @PathParam("currency") @Pattern(regexp = CURRENCY_REGEX, message = CURRENCY_MESSAGE) @NotBlank String currency,
            @Valid TransactionRequest request) {
        LedgerKey key = new LedgerKey(id, Currency.of(currency));

        try {
            Objects.requireNonNull(request, JSON_BODY_MESSAGE);

            return processTransaction(key, request, Transaction.Operation.DEPOSIT);
        } catch (Exception e) {
            Log.errorf("Error processing %s on service %s: %s", Transaction.Operation.DEPOSIT, key, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Log.infof("Finished processing %s on service %s", Transaction.Operation.DEPOSIT, key);
        }
    }

    @POST
    @Path("/{id}/transactions/{currency}/withdrawal")
    public Response withdraw(
            @PathParam("id") @NotNull UUID id,
            @PathParam("currency") @Pattern(regexp = CURRENCY_REGEX, message = CURRENCY_MESSAGE) @NotBlank String currency,
            @Valid TransactionRequest request) {
        LedgerKey key = new LedgerKey(id, Currency.of(currency));

        try {
            Objects.requireNonNull(request, JSON_BODY_MESSAGE);

            return processTransaction(key, request, Transaction.Operation.WITHDRAWAL);
        } catch (Exception e) {
            Log.errorf("Error processing %s on service %s: %s", Transaction.Operation.WITHDRAWAL, key, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Log.infof("Finished processing %s on service %s", Transaction.Operation.WITHDRAWAL, key);
        }
    }

    private Response processTransaction(LedgerKey key, TransactionRequest request, Transaction.Operation operation) {
        try {
            Log.infof("%s on service: %s amount %d", operation, key, request.getValue().longValue());

            if (!svc.exists(key)) {
                Log.warnf("Ledger not found: %s", key);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            Map.Entry<UUID, Transaction> trx = svc.commit(
                    key,
                    operation,
                    request.getValue(),
                    request.getDescription(),
                    request.getReference(),
                    key.currency());

            TransactionResponse dto = new TransactionResponse();

            dto.setId(trx.getKey());

            Log.infof("%s successful on service %s with transaction %s", operation, key, trx);

            return Response.status(Response.Status.CREATED).entity(dto).build();
        } catch (TransactionAlreadyExistsException e) {
            Log.errorf("Integrity error in service %s: %s", key, e);

            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch (ConstraintViolationException e) {
            String violations = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            ValidationResponse dto = new ValidationResponse();

            dto.setMessage(violations);

            Log.warnf("Validation error: %s", violations);

            return Response.status(Response.Status.BAD_REQUEST).entity(dto).build();
        }
    }
}
