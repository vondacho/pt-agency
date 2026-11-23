package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

/**
 * A participant's bookings, on the seeded sessions (db/dev/R__seed_sessions.sql).
 */
@TestHTTPEndpoint(ParticipantResource.class)
@QuarkusTest
class ParticipantResourceTest {

    /** Booked on the full small group session (day +1) and pre-booked on the bootcamp (day +2). */
    static final String PARTICIPANT = "20000000-0000-0000-0000-000000000002";

    @Test
    void lists_the_participant_s_coming_bookings_in_chronological_order() {
        given().when().get("/{id}/bookings", PARTICIPANT)
                .then().statusCode(200)
                .body("id", contains("00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000003"),
                        "participantBooking", contains("DONE", "PREBOOKED"));
    }

    @Test
    void lists_only_the_bookings_from_the_given_day() {
        given().queryParam("from", LocalDate.now().plusDays(2).toString())
                .when().get("/{id}/bookings", PARTICIPANT)
                .then().statusCode(200)
                .body("id", contains("00000000-0000-0000-0000-000000000003"));
    }

    @Test
    void a_participant_without_booking_has_none() {
        given().when().get("/{id}/bookings", UUID.randomUUID())
                .then().statusCode(200)
                .body("", empty());
    }
}
