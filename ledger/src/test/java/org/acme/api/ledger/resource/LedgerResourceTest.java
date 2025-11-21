package org.acme.api.ledger.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LedgerResourceTest {

    @Test
    public void testLedgersEndpoint() {
        String id = given()
                .contentType("application/json")
                .when().post("/api/v1/ledgers")
                .then()
                .statusCode(201)
                .extract().path("id");

        UUID uuid = UUID.fromString(id);

        assertNotNull(uuid);
    }

    @Test
    public void testAddCurrencyEndpoint() {
        String id = given()
                .contentType("application/json")
                .when().post("/api/v1/ledgers")
                .then()
                .statusCode(201)
                .extract().path("id");

        String currency = given()
                .contentType("application/json")
                .body("{\"currency\":\"USD\"}")
                .when().post("/api/v1/ledgers/" + id + "/currencies")
                .then()
                .statusCode(201)
                .extract().path("currency");

        assertEquals("USD", currency);
    }

    @Test
    public void testAddDepositEndpoint() {
        String id = given()
                .contentType("application/json")
                .when().post("/api/v1/ledgers")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("{\"value\":1000.00,\"description\":\"Crocery shopping\",\"reference\":\"DEP-001\"}")
                .when().post("/api/v1/ledgers/" + id + "/transactions/EUR/deposit")
                .then()
                .statusCode(201);

        Float balance = given()
                .contentType("application/json")
                .when().get("/api/v1/ledgers/" + id + "/balance/EUR")
                .then()
                .statusCode(200)
                .extract().path("balance");

        assertEquals(BigDecimal.valueOf(1000.00), BigDecimal.valueOf(balance));
    }

    @Test
    public void testHistoryEndpoint() {
        String id = given()
                .contentType("application/json")
                .when().post("/api/v1/ledgers")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("{\"value\":1000.00,\"description\":\"Crocery shopping\",\"reference\":\"DEP-001\"}")
                .when().post("/api/v1/ledgers/" + id + "/transactions/EUR/deposit")
                .then()
                .statusCode(201);

        List<Object> transactions = given()
                .contentType("application/json")
                .when().get("/api/v1/ledgers/" + id + "/transactions/EUR")
                .then()
                .statusCode(200)
                .extract().path("transactions");

        assertEquals(1, transactions.size());
    }
}
