package ch.obya.pta.booking.web;

import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.application.ClientProfile;
import ch.obya.pta.booking.application.EventPublisher;
import ch.obya.pta.booking.domain.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@TestHTTPEndpoint(BookingResource.class)
@QuarkusTest
public class BookingResourceTest {

    @InjectMock
    SessionRepository sessionRepository;
    @InjectMock
    ArticleStore articleStore;
    @InjectMock
    ClientProfile clientProfile;
    @InjectMock
    EventPublisher eventPublisher;

    @Test
    void sessionNotFound() {
        var session = sessionTest();
        when(sessionRepository.findById(any())).thenReturn(Uni.createFrom().nothing());
        given()
                .when()
                .get("/{id}", Map.of("id", session.id().id()))
                .then()
                .log().all()
                .statusCode(404)
                .body("detail", equalTo("Session %s does not exist".formatted(session.id())));
    }

    @Test
    void lookUpOneSession() {
        var session = sessionTest();
        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        given()
                .when()
                .get("/{id}", Map.of("id", session.id().id()))
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(session.id().id().toString()));
    }

    @Test
    void bookOneSession() {
        var session = sessionTest();
        var participant = participantTest();
        var subscription = subscriptionTest();

        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.getSubscriptions(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription)));
        when(articleStore.selectMatchingSubscriptions(anySet(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription.articleId())));
        when(eventPublisher.send(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .post("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .log().all()
                .statusCode(201)
                .body(
                        "session.id", equalTo(session.id().id().toString()),
                        "participant.id", equalTo(participant.id().toString()));
    }

    @Test
    void cancelBooking() {
        var participant = participantTest();
        var subscription = subscriptionTest();
        var session = sessionTest(participant, subscription.id());
        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        given()
                .when()
                .delete("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .log().all()
                .statusCode(204);
    }

    private Session sessionTest() {
        return sessionTest(null, null);
    }

    private Session sessionTest(ParticipantId withParticipant, SubscriptionId withSubscription) {
        var id = new SessionId(UUID.randomUUID());
        return new Session(
                id,
                new ArticleId(UUID.randomUUID()),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(3, 25),
                new HashSet<>(withParticipant != null ? Set.of(bookingTest(id, withParticipant, withSubscription)) : Collections.emptySet()));
    }

    private Booking bookingTest(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Booking.Status.DONE);
    }

    private Subscription subscriptionTest() {
        var today = LocalDate.now();
        return new Subscription(
                new SubscriptionId(UUID.randomUUID()),
                new ArticleId(UUID.randomUUID()),
                new Subscription.Validity(today, today.plusMonths(12)),
                null);
    }

    private ParticipantId participantTest() {
        return new ParticipantId(UUID.randomUUID());
    }
}
