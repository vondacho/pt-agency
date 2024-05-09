package ch.obya.pta.booking.infrastructure.web;

/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.application.ClientProfile;
import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.vo.*;
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

@TestHTTPEndpoint(SessionResource.class)
@QuarkusTest
public class SessionResourceTest {

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
        var id = SessionId.create();
        return new Session(
                id,
                ArticleId.create(),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(3, 25),
                new HashSet<>(withParticipant != null ? Set.of(bookingTest(id, withParticipant, withSubscription)) : Collections.emptySet()));
    }

    private Booking bookingTest(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return Booking.done(session, participant, subscription);
    }

    private Subscription subscriptionTest() {
        var today = LocalDate.now();
        return new Subscription(
                SubscriptionId.create(),
                ArticleId.create(),
                new Subscription.Validity(today, today.plusMonths(12)),
                null);
    }

    private ParticipantId participantTest() {
        return ParticipantId.create();
    }
}
