package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.application.BookingService;
import ch.obya.pta.booking.domain.util.Samples;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.SubscriptionId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.booking.domain.util.Samples.onePrivateSession;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestHTTPEndpoint(SessionResource.class)
@QuarkusTest
public class SessionResourceTest {

    @InjectMock
    BookingService bookingService;

    @Mock
    EventPublisher eventPublisher() {
        return events -> Uni.createFrom().voidItem();
    }

    @Test
    void look_up_non_existing_session_resource_should_fail_with_404() {
        var session = SessionId.create();
        when(bookingService.findOne(any())).thenReturn(Uni.createFrom()
                .failure(CommonProblem.EntityNotFound.toException("Session", session.id()).get()));
        given()
                .when()
                .get("/{id}", Map.of("id", session.id()))
                .then()
                .log().all()
                .statusCode(404)
                .body("detail", equalTo("Session %s does not exist.".formatted(session.id())));
    }

    @Test
    void look_up_existing_session_resource_should_succeed_with_200() {
        var session = onePrivateSession.get();
        when(bookingService.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        given()
                .when()
                .get("/{id}", Map.of("id", session.id().id()))
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(session.id().id().toString()),
                        "minSeats", equalTo(1),
                        "maxSeats", equalTo(1),
                        "bookedSeats", equalTo(0),
                        "availableSeats", equalTo(1),
                        "confirmed", equalTo(false));
    }

    @Test
    void booking_session_should_should_succeed_with_201() {
        var session = onePrivateSession.get();
        var participant = Samples.oneParticipant.get();

        when(bookingService.book(session.id(), participant)).thenReturn(
                Uni.createFrom().item(Booking.done(session.id(), participant, SubscriptionId.create())));

        given()
                .when()
                .post("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .log().all()
                .statusCode(201)
                .body(
                        "session.id", equalTo(session.id().id().toString()),
                        "participant.id", equalTo(participant.id().toString()),
                        "status", equalTo("DONE"));
    }

    @Test
    void cancel_booking_should_should_succeed_with_204() {
        var session = Samples.oneSmallGroupSession.get();
        var participant = Samples.oneParticipant.get();

        when(bookingService.cancel(session.id(), participant)).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .delete("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .log().all()
                .statusCode(204);
    }

    @Test
    void remove_existing_session_resource_should_succeed_with_204() {
        var session = onePrivateSession.get();
        when(bookingService.remove(session.id())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .delete("/{id}", Map.of("id", session.id().id()))
                .then()
                .log().all()
                .statusCode(204);
    }


    @Test
    void list_sessions_in_period_should_return_seeded_sessions_in_chronological_order_with_seat_availability() {
        var tomorrow = LocalDate.now().plusDays(1);
        given()
                .when()
                .queryParam("from", tomorrow.toString())
                .queryParam("to", tomorrow.plusDays(2).toString())
                .get()
                .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(4),
                        "id", contains(
                                "00000000-0000-0000-0000-000000000001",
                                "00000000-0000-0000-0000-000000000002",
                                "00000000-0000-0000-0000-000000000003",
                                "00000000-0000-0000-0000-000000000004"),
                        // small group, full with one participant on the waiting list
                        "[1].maxSeats", equalTo(5),
                        "[1].bookedSeats", equalTo(5),
                        "[1].availableSeats", equalTo(0),
                        "[1].waitlisted", equalTo(1),
                        "[1].confirmed", equalTo(true),
                        // bootcamp, minimum of 3 not reached yet
                        "[2].minSeats", equalTo(3),
                        "[2].bookedSeats", equalTo(2),
                        "[2].availableSeats", equalTo(18),
                        "[2].confirmed", equalTo(false));
    }

    @Test
    void list_sessions_without_period_should_default_to_the_coming_week() {
        given()
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("id", hasItems(
                                "00000000-0000-0000-0000-000000000001",
                                "00000000-0000-0000-0000-000000000007"),
                        "id", not(hasItem("00000000-0000-0000-0000-000000000008")),
                        "day", everyItem(lessThanOrEqualTo(LocalDate.now().plusDays(7).toString())));
    }

    @Test
    void list_sessions_with_inverted_period_should_fail_with_400() {
        given()
                .when()
                .queryParam("from", LocalDate.now().toString())
                .queryParam("to", LocalDate.now().minusDays(1).toString())
                .get()
                .then()
                .statusCode(400);
    }


    @Test
    void booking_a_session_changed_concurrently_should_fail_with_409() {
        var session = onePrivateSession.get();
        var participant = Samples.oneParticipant.get();
        when(bookingService.book(session.id(), participant)).thenReturn(
                Uni.createFrom().failure(new org.hibernate.StaleObjectStateException("training_session", 1L)));

        given()
                .when()
                .post("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .statusCode(409)
                .body("code", equalTo("ConcurrentModification"));
    }
}
