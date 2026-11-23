package ch.obya.pta.customer.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

/**
 * The contract pta-booking relies on, against the seeded customers (db/dev/R__seed_customers.sql), through the real service.
 */
@TestHTTPEndpoint(CustomerResource.class)
@QuarkusTest
class CustomerSubscriptionsTest {

    static final String ALICE = "20000000-0000-0000-0000-00000000000a";
    static final String BOB = "20000000-0000-0000-0000-00000000000b";

    @Test
    void subscriptions_valid_today_should_be_listed() {
        given().queryParam("at", LocalDate.now().toString())
                .when().get("/{id}/subscriptions", ALICE)
                .then().statusCode(200)
                .body("articleId.id", contains("50000000-0000-0000-0000-000000000001"));
    }

    @Test
    void subscriptions_should_not_be_listed_outside_their_validity() {
        given().queryParam("at", LocalDate.now().plusYears(2).toString())
                .when().get("/{id}/subscriptions", ALICE)
                .then().statusCode(200)
                .body("", empty());
    }

    @Test
    void customer_without_subscription_should_have_none() {
        given().queryParam("at", LocalDate.now().toString())
                .when().get("/{id}/subscriptions", BOB)
                .then().statusCode(200)
                .body("", empty());
    }

    @Test
    void subscriptions_of_an_unknown_customer_should_fail_with_404() {
        given().queryParam("at", LocalDate.now().toString())
                .when().get("/{id}/subscriptions", UUID.randomUUID())
                .then().statusCode(404);
    }
}
