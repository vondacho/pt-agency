package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.smallrye.mutiny.Uni;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Booking on behalf of the signed-in participant: the participant id comes from the token, never from the request.
 */
@QuarkusTest
class BookingActionsTest {

    static final String ALICE = "20000000-0000-0000-0000-00000000000a";
    static final UUID SESSION = UUID.fromString("f0000000-0000-0000-0000-000000000001");

    @InjectMock
    @RestClient
    BookingClient booking;

    private static BookingView booked(String status) {
        return new BookingView(new BookingView.Ref(SESSION), new BookingView.Ref(UUID.fromString(ALICE)), status);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void signed_in_participant_books_for_themselves() {
        when(booking.book(SESSION, UUID.fromString(ALICE))).thenReturn(Uni.createFrom().item(booked("DONE")));

        given().header("X-PTA-Request", "1")
                .when().post("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(201)
                .body("session", equalTo(SESSION.toString()), "status", equalTo("DONE"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void signed_in_participant_cancels_their_booking() {
        when(booking.cancel(SESSION, UUID.fromString(ALICE))).thenReturn(Uni.createFrom().voidItem());

        given().header("X-PTA-Request", "1")
                .when().delete("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(204);
        verify(booking).cancel(SESSION, UUID.fromString(ALICE));
    }

    @Test
    void anonymous_visitor_cannot_book() {
        given().header("X-PTA-Request", "1")
                .when().post("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(401)
                .contentType("application/problem+json");
        verify(booking, never()).book(any(), any());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void booking_without_the_request_header_is_rejected() {
        given().when().post("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(403);
        verify(booking, never()).book(any(), any());
    }

    @Test
    @TestSecurity(user = "carol")
    void account_not_linked_to_a_participant_cannot_book() {
        given().header("X-PTA-Request", "1")
                .when().post("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(403)
                .body("detail", equalTo("Your account is not linked to a participant yet. Please contact the agency."));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = "20000000-0000-0000-0000-00000000000b"))
    void missing_subscription_is_explained() {
        when(booking.book(any(), any())).thenReturn(Uni.createFrom().failure(new WebApplicationException(422)));

        given().header("X-PTA-Request", "1")
                .when().post("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(422)
                .body("detail", equalTo("You have no active subscription valid for this session."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void concurrent_change_asks_to_retry() {
        when(booking.book(any(), any())).thenReturn(Uni.createFrom().failure(new WebApplicationException(409)));

        given().header("X-PTA-Request", "1")
                .when().post("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(409)
                .body("detail", equalTo("This session has just changed. Please try again."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void sessions_tell_the_signed_in_participant_their_bookings() {
        var day = LocalDate.now();
        var session = new SessionView(SESSION, "Personal training", day, LocalTime.of(7, 0), LocalTime.of(8, 0),
                "Studio A", 1, 1, 1, 0, 0, true, "DONE");
        when(booking.sessions(any(), any(), eq(UUID.fromString(ALICE)))).thenReturn(Uni.createFrom().item(List.of(session)));

        given().when().get("/api/sessions")
                .then().statusCode(200)
                .body("[0].participantBooking", equalTo("DONE"));
    }

    @Test
    void anonymous_visitors_see_sessions_without_participant() {
        when(booking.sessions(any(), any(), isNull())).thenReturn(Uni.createFrom().item(List.of()));

        given().when().get("/api/sessions").then().statusCode(200);
        verify(booking).sessions(any(), any(), isNull());
    }
}
