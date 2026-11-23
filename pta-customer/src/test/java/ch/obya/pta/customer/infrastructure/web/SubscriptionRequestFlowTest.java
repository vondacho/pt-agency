package ch.obya.pta.customer.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Requesting, cancelling and confirming subscriptions, through the real service and database (seeded customers),
 * with pta-shopping stubbed by WireMock. Each test uses its own article, so they do not interfere.
 */
@QuarkusTest
@ConnectWireMock
class SubscriptionRequestFlowTest {

    static final String BOB = "20000000-0000-0000-0000-00000000000b";

    WireMock wiremock;

    /** An article of the catalogue, as pta-shopping represents it. */
    private String article(String durationTag, String quota) {
        var id = UUID.randomUUID().toString();
        wiremock.register(get(urlPathEqualTo("/articles/" + id)).willReturn(okJson("""
                {"id":{"id":"%s"},"name":{"content":"Pass"},"profile":[{"name":"SUBSCRIPTION"}%s],
                 "grants":[{"name":"SESSION"},{"name":"COURSE"}]%s}"""
                .formatted(id, durationTag == null ? "" : ",{\"name\":\"" + durationTag + "\"}",
                        quota == null ? "" : ",\"quota\":" + quota))));
        return id;
    }

    private String request(String article) {
        return given().contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(article))
                .when().post("/customers/{id}/subscription-requests", BOB)
                .then().statusCode(201)
                .body("status", equalTo("PENDING"), "articleId", equalTo(article))
                .extract().path("id");
    }

    @Test
    void a_confirmed_request_becomes_a_subscription_for_the_article_duration() {
        var monthly = article("ONE_MONTH", null);
        var request = request(monthly);
        var from = LocalDate.now().plusDays(3);

        given().queryParam("from", from.toString())
                .when().post("/customers/{id}/subscription-requests/{r}/confirmation", BOB, request)
                .then().statusCode(200)
                .body("status", equalTo("CONFIRMED"));

        given().queryParam("at", from.plusDays(10).toString())
                .when().get("/customers/{id}/subscriptions", BOB)
                .then().statusCode(200)
                .body("find { it.articleId.id == '%s' }.validity.from".formatted(monthly), equalTo(from.toString()),
                        "find { it.articleId.id == '%s' }.validity.to".formatted(monthly), equalTo(from.plusMonths(1).minusDays(1).toString()));
    }

    @Test
    void a_pass_includes_its_sessions() {
        var card = article("ONE_YEAR", "{\"min\":0,\"max\":10}");
        var request = request(card);

        given().when().post("/customers/{id}/subscription-requests/{r}/confirmation", BOB, request).then().statusCode(200);

        given().when().get("/customers/{id}/subscriptions", BOB)
                .then().statusCode(200)
                .body("find { it.articleId.id == '%s' }.credits".formatted(card), equalTo(10));
    }

    @Test
    void requests_are_listed_and_a_pending_one_can_be_cancelled_once() {
        var trial = article("ONE_WEEK", null);
        var request = request(trial);

        given().when().get("/customers/{id}/subscription-requests", BOB)
                .then().statusCode(200)
                .body("id", hasItem(request));

        given().when().delete("/customers/{id}/subscription-requests/{r}", BOB, request)
                .then().statusCode(200).body("status", equalTo("CANCELLED"));
        given().when().delete("/customers/{id}/subscription-requests/{r}", BOB, request)
                .then().statusCode(422).body("code", equalTo("RequestNotPending"));
        given().when().post("/customers/{id}/subscription-requests/{r}/confirmation", BOB, request)
                .then().statusCode(422);
        given().when().get("/customers/{id}/subscriptions", BOB)
                .then().body("articleId.id", not(hasItem(trial)));
    }

    @Test
    void a_second_pending_request_for_the_same_article_is_a_conflict() {
        var monthly = article("ONE_MONTH", null);
        request(monthly);

        given().contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(monthly))
                .when().post("/customers/{id}/subscription-requests", BOB)
                .then().statusCode(409).body("code", equalTo("RequestAlreadyPending"));
    }

    @Test
    void an_article_outside_the_catalogue_cannot_be_requested() {
        var unknown = UUID.randomUUID().toString();
        wiremock.register(get(urlPathEqualTo("/articles/" + unknown)).willReturn(aResponse().withStatus(404)));

        given().contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(unknown))
                .when().post("/customers/{id}/subscription-requests", BOB)
                .then().statusCode(422).body("code", equalTo("UnknownArticle"));
    }

    @Test
    void an_unavailable_catalogue_answers_502() {
        var article = UUID.randomUUID().toString();
        wiremock.register(get(urlPathEqualTo("/articles/" + article)).willReturn(aResponse().withStatus(503)));

        given().contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(article))
                .when().post("/customers/{id}/subscription-requests", BOB)
                .then().statusCode(502).body("code", equalTo("ServiceUnavailable"));
    }

    @Test
    void requests_of_an_unknown_customer_are_not_found() {
        given().when().get("/customers/{id}/subscription-requests", UUID.randomUUID())
                .then().statusCode(404);
    }
}
