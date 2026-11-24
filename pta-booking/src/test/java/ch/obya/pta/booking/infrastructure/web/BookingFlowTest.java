package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.application.CustomerProfile;
import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.vo.ArticleId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;
import ch.obya.pta.booking.domain.vo.SubscriptionId;
import ch.obya.pta.common.domain.vo.Validity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Booking flows through the real service and repository (PostgreSQL from Dev Services), with the participant's
 * subscriptions (pta-customer) and the eligible articles (pta-shopping) mocked. Uses the sessions of db/test.
 */
@TestHTTPEndpoint(SessionResource.class)
@QuarkusTest
class BookingFlowTest {

    static final String PRIVATE = "f0000000-0000-0000-0000-000000000001";
    static final String GROUP_NO_SUBSCRIPTION = "f0000000-0000-0000-0000-000000000002";
    static final String GROUP_CUSTOMER_DOWN = "f0000000-0000-0000-0000-000000000003";
    static final String BOOTCAMP = "f0000000-0000-0000-0000-000000000004";
    static final String CANCELLATION = "f0000000-0000-0000-0000-000000000005";
    static final ArticleId PERSONAL_TRAINING = new ArticleId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    static final ArticleId BOOTCAMP_ARTICLE = new ArticleId(UUID.fromString("10000000-0000-0000-0000-000000000003"));
    static final ArticleId UNLIMITED_PASS = new ArticleId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
    static final LocalDate DAY = LocalDate.now().plusDays(30);

    @InjectMock
    CustomerProfile customerProfile;

    @InjectMock
    ArticleStore articleStore;

    @org.junit.jupiter.api.BeforeEach
    void courseDeadline() {
        // the test sessions start in 30 days: a 12 hours deadline leaves cancellation open
        when(articleStore.cancellationHoursOf(any())).thenReturn(Uni.createFrom().item(12));
    }

        private void holdsUnlimitedPass(UUID participant) {
        when(customerProfile.validSubscriptionsOf(eq(new ParticipantId(participant)), any())).thenReturn(Multi.createFrom().item(
                new Subscription(SubscriptionId.create(), UNLIMITED_PASS, new Validity(DAY.minusMonths(1), DAY.plusMonths(1)))));
    }

    private void passGivesAccessTo(ArticleId article) {
        when(articleStore.eligibleSubscriptionsFor(article)).thenReturn(Multi.createFrom().item(UNLIMITED_PASS));
    }

    private void participantBookingIs(String session, UUID participant, String status) {
        given().queryParam("from", DAY.toString()).queryParam("to", DAY.toString()).queryParam("participant", participant)
                .when().get()
                .then().statusCode(200)
                .body("find { it.id == '%s' }.participantBooking".formatted(session), equalTo(status));
    }

    @Test
    void book_join_the_waiting_list_and_cancel() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        holdsUnlimitedPass(first);
        holdsUnlimitedPass(second);
        passGivesAccessTo(PERSONAL_TRAINING);

        given().when().post("/{id}/bookings/{participant}", PRIVATE, first)
                .then().statusCode(201).body("status", equalTo("DONE"));
        participantBookingIs(PRIVATE, first, "DONE");

        // the private session is full now: the next participant goes on the waiting list
        given().when().post("/{id}/bookings/{participant}", PRIVATE, second)
                .then().statusCode(201).body("status", equalTo("WAITING"));
        participantBookingIs(PRIVATE, second, "WAITING");

        // cancelling frees the seat, which goes to the waiting participant
        given().when().delete("/{id}/bookings/{participant}", PRIVATE, first).then().statusCode(204);
        participantBookingIs(PRIVATE, second, "DONE");
        given().queryParam("from", DAY.toString()).queryParam("to", DAY.toString()).queryParam("participant", first)
                .when().get()
                .then().statusCode(200)
                .body("find { it.id == '%s' }.participantBooking".formatted(PRIVATE), equalTo(null));
    }

    @Test
    void booking_below_the_minimum_should_pre_book() {
        var participant = UUID.randomUUID();
        holdsUnlimitedPass(participant);
        passGivesAccessTo(BOOTCAMP_ARTICLE);

        given().when().post("/{id}/bookings/{participant}", BOOTCAMP, participant)
                .then().statusCode(201).body("status", equalTo("PREBOOKED"));
    }

    @Test
    void booking_without_eligible_subscription_should_fail_with_422() {
        var participant = UUID.randomUUID();
        when(customerProfile.validSubscriptionsOf(any(), any())).thenReturn(Multi.createFrom().empty());

        given().when().post("/{id}/bookings/{participant}", GROUP_NO_SUBSCRIPTION, participant)
                .then().statusCode(422)
                .contentType("application/problem+json")
                .body("code", equalTo("NoActiveSubscription"));
    }

    @Test
    void unavailable_customer_service_should_fail_with_502_not_as_missing_subscription() {
        when(customerProfile.validSubscriptionsOf(any(), any()))
                .thenReturn(Multi.createFrom().failure(BookingProblem.ServiceUnavailable.toException("pta-customer", "Connection refused").get()));

        given().when().post("/{id}/bookings/{participant}", GROUP_CUSTOMER_DOWN, UUID.randomUUID())
                .then().statusCode(502)
                .body("code", equalTo("ServiceUnavailable"));
    }

    @Test
    void cancelling_a_booking_that_does_not_exist_should_fail_with_404() {
        given().when().delete("/{id}/bookings/{participant}", GROUP_NO_SUBSCRIPTION, UUID.randomUUID())
                .then().statusCode(404)
                .body("code", equalTo("NoBooking"));
    }

    @Test
    void unknown_session_should_answer_404() {
        // regression: an unknown session used to answer 500 (null entity instead of "not found")
        given().when().get("/{id}", UUID.randomUUID())
                .then().statusCode(404)
                .body("code", equalTo("EntityNotFound"));
        given().when().post("/{id}/bookings/{participant}", UUID.randomUUID(), UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void cancelling_a_booked_seat_after_the_course_deadline_should_fail_with_422() {
        var participant = UUID.randomUUID();
        holdsUnlimitedPass(participant);
        passGivesAccessTo(BOOTCAMP_ARTICLE);
        given().when().post("/{id}/bookings/{participant}", CANCELLATION, participant)
                .then().statusCode(201).body("status", equalTo("DONE"));

        // a deadline long before the session's start: the booked seat can no longer be cancelled
        when(articleStore.cancellationHoursOf(any())).thenReturn(Uni.createFrom().item(24 * 365));
        given().when().delete("/{id}/bookings/{participant}", CANCELLATION, participant)
                .then().statusCode(422)
                .body("code", equalTo("CancellationClosed"));
        participantBookingIs(CANCELLATION, participant, "DONE");
    }
}
