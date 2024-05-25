package ch.obya.pta.booking.application;

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

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.event.*;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.util.Samples;
import ch.obya.pta.booking.domain.vo.BookingId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    EventPublisher eventPublisher;
    @Mock
    ArticleStore articleStore;
    @Mock
    CustomerProfile clientProfile;
    @Mock
    SessionRepository sessionRepository;
    @InjectMocks
    BookingService bookingService;
    @Captor
    ArgumentCaptor<Collection<Event>> eventCaptor;
    @Captor
    ArgumentCaptor<Session> persistedSessionCaptor;

    @Test
    void look_up_non_existing_session_should_fail() {
        var session = SessionId.create();

        when(sessionRepository.findOne(session)).thenReturn(Uni.createFrom().nothing());

        var result = bookingService.findOne(session);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(CommonProblem.Exception.class, "Session %s does not exist.".formatted(session));
    }

    @Test
    void look_up_session_should_return_expected_entity() {
        var session = Samples.onePrivateSession.get();

        when(sessionRepository.findOne(session.id())).thenReturn(Uni.createFrom().item(session));

        var result = bookingService.findOne(session.id()).subscribe().withSubscriber(UniAssertSubscriber.create()).getItem();

        assertThat(result.id()).isEqualTo(session.id());
    }

    @Test
    void booking_non_session_should_fail() {
        var session = SessionId.create();
        var participant = Samples.oneParticipant.get();

        when(sessionRepository.findOne(any())).thenReturn(Uni.createFrom().nothing());

        var result = bookingService.book(session, participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(BookingProblem.Exception.class, "Session %s does not exist.".formatted(session));
    }

    @Test
    void booking_session_without_subscription_should_fail() {
        var session = Samples.onePrivateSession.get();
        var participant = Samples.oneParticipant.get();

        when(sessionRepository.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.validSubscriptionsOf(any(), any())).thenReturn(Multi.createFrom().nothing());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(BookingProblem.Exception.class,
                        "No active subscription of participant %s applies to session %s (article %s)"
                                .formatted(participant, session.id(), session.articleId()));
    }

    @Test
    void booking_session_without_compatible_subscription_should_fail() {
        var session = Samples.onePrivateSession.get();
        var participant = Samples.oneParticipant.get();
        var subscription = Samples.oneSubscription.get();

        when(sessionRepository.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.validSubscriptionsOf(any(), any())).thenReturn(Multi.createFrom().items(subscription));
        when(articleStore.eligibleSubscriptionsFor(any())).thenReturn(Multi.createFrom().nothing());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(BookingProblem.Exception.class,
                        "No active subscription of participant %s applies to session %s (article %s)"
                                .formatted(participant, session.id(), session.articleId()));
    }

    @Test
    void booking_session_return_booking_entity_and_charge_subscription() {
        var session = Samples.onePrivateSession.get();
        var participant = Samples.oneParticipant.get();
        var subscription = Samples.oneSubscription.get();

        when(sessionRepository.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.validSubscriptionsOf(any(), any())).thenReturn(Multi.createFrom().items(subscription));
        when(articleStore.eligibleSubscriptionsFor(any())).thenReturn(Multi.createFrom().items(subscription.articleId()));
        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(new BookingId(session.id(), participant));

        verify(sessionRepository, times(1)).save(persistedSessionCaptor.capture());
        assertThat(persistedSessionCaptor.getValue().id()).isEqualTo(session.id());

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactlyInAnyOrder(
                    new SessionBooked(session.id(), participant),
                    new SubscriptionCharged(subscription.id(), participant));
    }

    @Test
    void booking_full_session_return_one_booking_with_status_waiting() {
        var session = Samples.onePrivateSession.get();
        session.book(Samples.oneParticipant.get(), Samples.oneSubscription.get().id());
        session.domainEvents();

        var participant = Samples.oneParticipant.get();
        var subscription = Samples.oneSubscription.get();

        when(sessionRepository.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.validSubscriptionsOf(any(), any())).thenReturn(Multi.createFrom().items(subscription));
        when(articleStore.eligibleSubscriptionsFor(any())).thenReturn(Multi.createFrom().items(subscription.articleId()));
        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(new BookingId(session.id(), participant));

        verify(sessionRepository, times(1)).save(persistedSessionCaptor.capture());
        assertThat(persistedSessionCaptor.getValue().id()).isEqualTo(session.id());

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new ParticipantWaitlisted(session.id(), participant));
    }

    @Test
    void cancelling_booking_credits_used_subscription() {
        var session = Samples.onePrivateSession.get();
        var participant = Samples.oneParticipant.get();
        var subscription = Samples.oneSubscription.get();
        session.book(participant, subscription.id());
        session.domainEvents();

        when(sessionRepository.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = bookingService.cancel(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();

        verify(sessionRepository, times(1)).save(persistedSessionCaptor.capture());
        assertThat(persistedSessionCaptor.getValue().id()).isEqualTo(session.id());

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactlyInAnyOrder(
                    new BookingCancelled(session.id(), participant),
                    new SubscriptionCredited(subscription.id(), participant));
    }
}
