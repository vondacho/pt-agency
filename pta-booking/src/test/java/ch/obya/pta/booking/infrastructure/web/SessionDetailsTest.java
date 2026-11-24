package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * A session's details, on the seeded data (db/dev), with pta-shopping stubbed by WireMock.
 */
@QuarkusTest
@ConnectWireMock
class SessionDetailsTest {

    /** Bootcamp on day +2 at 12:15, with Marco at the Parc de Milan and a note. */
    static final String BOOTCAMP = "00000000-0000-0000-0000-000000000003";
    static final String BOOTCAMP_ARTICLE = "10000000-0000-0000-0000-000000000003";

    WireMock wiremock;

    @BeforeEach
    void reset() {
        wiremock.resetMappings();
    }

    @Test
    void details_tell_the_instructor_the_venue_the_note_and_the_cancellation_deadline() {
        wiremock.register(get(urlPathEqualTo("/articles/" + BOOTCAMP_ARTICLE)).willReturn(okJson("""
                {"id":{"id":"%s"},"name":{"content":"Bootcamp"},"course":{"objectives":["Outdoor circuits"],"cancellationHours":12}}"""
                .formatted(BOOTCAMP_ARTICLE))));
        var start = LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(12, 15));

        given().when().get("/sessions/{id}/details", BOOTCAMP)
                .then().statusCode(200)
                .body("session.title", equalTo("Bootcamp"),
                        "session.instructor", equalTo("Marco Rossi"),
                        "articleId", equalTo(BOOTCAMP_ARTICLE),
                        "instructor.name", equalTo("Marco Rossi"),
                        "instructor.bio", startsWith("Group coach"),
                        "venue.name", equalTo("Parc de Milan"),
                        "venue.address", equalTo("Avenue de Milan, 1007 Lausanne"),
                        "venue.mapUrl", startsWith("https://www.openstreetmap.org/"),
                        "note", equalTo("New playlist and a partner challenge at the end."),
                        "cancellationHours", equalTo(12),
                        "cancellableUntil", startsWith(start.minusHours(12).toString().substring(0, 16)));
    }

    @Test
    void details_stay_available_without_the_catalogue() {
        wiremock.register(get(urlPathEqualTo("/articles/" + BOOTCAMP_ARTICLE)).willReturn(aResponse().withStatus(503)));

        given().when().get("/sessions/{id}/details", BOOTCAMP)
                .then().statusCode(200)
                .body("venue.name", equalTo("Parc de Milan"),
                        "cancellationHours", nullValue(),
                        "cancellableUntil", nullValue());
    }

    @Test
    void sessions_list_their_instructor() {
        given().queryParam("from", LocalDate.now().plusDays(2).toString()).queryParam("to", LocalDate.now().plusDays(2).toString())
                .when().get("/sessions")
                .then().statusCode(200)
                .body("find { it.id == '%s' }.instructor".formatted(BOOTCAMP), equalTo("Marco Rossi"));
    }

    @Test
    void instructors_and_venues_can_be_read() {
        given().when().get("/instructors/{id}", "60000000-0000-0000-0000-000000000001")
                .then().statusCode(200).body("name", equalTo("Léa Dubois"));
        given().when().get("/venues/{id}", "70000000-0000-0000-0000-000000000001")
                .then().statusCode(200).body("address", equalTo("Rue du Lac 12, 1003 Lausanne"));
        given().when().get("/venues/{id}", UUID.randomUUID()).then().statusCode(404);
        given().when().get("/sessions/{id}/details", UUID.randomUUID()).then().statusCode(404);
    }
}
