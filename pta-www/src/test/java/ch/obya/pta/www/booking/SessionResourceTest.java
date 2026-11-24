package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class SessionResourceTest {

    @InjectMock
    @RestClient
    BookingClient booking;

    @Test
    void sessions_should_be_relayed_from_booking() {
        var day = LocalDate.of(2026, 11, 23);
        var full = new SessionView(UUID.randomUUID(), "Small group HIIT", day, LocalTime.of(18, 0), LocalTime.of(19, 0),
                "Studio B", 2, 5, 5, 0, 1, true, null, null);
        var unlimited = new SessionView(UUID.randomUUID(), "Open gym", day, LocalTime.of(20, 0), LocalTime.of(21, 30),
                "Studio A", null, null, 3, null, 0, true, null, null);
        when(booking.sessions(day, day.plusDays(6), null)).thenReturn(Uni.createFrom().item(List.of(full, unlimited)));

        given()
                .queryParam("from", "2026-11-23")
                .queryParam("to", "2026-11-29")
                .when()
                .get("/api/sessions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2),
                        "[0].title", equalTo("Small group HIIT"),
                        "[0].day", equalTo("2026-11-23"),
                        "[0].start", equalTo("18:00:00"),
                        "[0].availableSeats", equalTo(0),
                        "[0].waitlisted", equalTo(1),
                        "[1].maxSeats", nullValue(),
                        "[1].availableSeats", nullValue());
    }

    @Test
    void invalid_period_rejected_by_booking_should_be_relayed_as_400() {
        when(booking.sessions(any(), any(), any())).thenReturn(Uni.createFrom().failure(new WebApplicationException(400)));

        given()
                .queryParam("from", "2026-11-23")
                .queryParam("to", "2026-11-01")
                .when()
                .get("/api/sessions")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400));
    }

    @Test
    void unavailable_booking_should_answer_502() {
        when(booking.sessions(any(), any(), any())).thenReturn(Uni.createFrom().failure(new ProcessingException("Connection refused")));

        given()
                .when()
                .get("/api/sessions")
                .then()
                .statusCode(502)
                .contentType("application/problem+json")
                .body("detail", equalTo("Sessions are temporarily unavailable."));
    }
}
