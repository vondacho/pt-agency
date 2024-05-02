package ch.obya.pta.booking.application;

import ch.obya.pta.booking.domain.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    EventPublisher eventPublisher;
    @Mock
    ArticleStore articleStore;
    @Mock
    ClientProfile clientProfile;
    @Mock
    SessionRepository sessionRepository;
    @InjectMocks
    BookingService bookingService;
    @Captor
    ArgumentCaptor<Collection<DomainEvent>> eventCaptor;
    @Captor
    ArgumentCaptor<Session> persistedSessionCaptor;

    @Test
    void sessionDoesNotExist() {
        var session = sessionTest(1,1);
        var participant = participantTest();

        when(sessionRepository.findById(any())).thenReturn(Uni.createFrom().nothing());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(BookingProblem.Exception.class, "Session %s does not exist.".formatted(session.id()));
    }

    @Test
    void participantWithoutActiveSubscription() {
        var session = sessionTest(1,1);
        var participant = participantTest();

        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.getSubscriptions(any(), any())).thenReturn(Uni.createFrom().nothing());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(BookingProblem.Exception.class,
                        "No active subscription of participant %s applies to session %s (article %s)"
                                .formatted(participant, session.id(), session.articleId()));
    }

    @Test
    void noActiveSubscriptionCanApplyToSession() {
        var session = sessionTest(1,1);
        var participant = participantTest();
        var subscription = subscriptionTest();

        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.getSubscriptions(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription)));
        when(articleStore.selectMatchingSubscriptions(anySet(), any()))
                .thenReturn(Uni.createFrom().nothing());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(BookingProblem.Exception.class,
                        "No active subscription of participant %s applies to session %s (article %s)"
                                .formatted(participant, session.id(), session.articleId()));
    }

    @Test
    void participantBooksOneSession() {
        var session = sessionTest(1,1);
        var participant = participantTest();
        var subscription = subscriptionTest();

        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.getSubscriptions(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription)));
        when(articleStore.selectMatchingSubscriptions(anySet(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription.articleId())));
        when(eventPublisher.send(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(new BookingId(session.id(), participant));

        verify(eventPublisher, times(1)).send(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).containsExactly(
                new SessionBooked(session.id(), participant),
                new SubscriptionCharged(subscription.id(), participant));

        verify(sessionRepository, times(1)).persist(persistedSessionCaptor.capture());
        assertThat(persistedSessionCaptor.getValue().id()).isEqualTo(session.id());
    }

    @Test
    void participantBooksOneFullSession() {
        var session = sessionTest(1,1, participantTest(), subscriptionTest().id());
        var participant = participantTest();
        var subscription = subscriptionTest();

        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        when(clientProfile.getSubscriptions(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription)));
        when(articleStore.selectMatchingSubscriptions(anySet(), any()))
                .thenReturn(Uni.createFrom().item(List.of(subscription.articleId())));
        when(eventPublisher.send(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = bookingService.book(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(new BookingId(session.id(), participant));

        verify(eventPublisher, times(1)).send(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).containsExactly(new ParticipantWaitlisted(session.id(), participant));

        verify(sessionRepository, times(1)).persist(persistedSessionCaptor.capture());
        assertThat(persistedSessionCaptor.getValue().id()).isEqualTo(session.id());
    }

    @Test
    void participantCancelOneBooking() {
        var participant = participantTest();
        var subscription = subscriptionTest();
        var session = sessionTest(1,1, participant, subscription.id());

        when(sessionRepository.findById(session.id())).thenReturn(Uni.createFrom().item(session));
        when(eventPublisher.send(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = bookingService.cancel(session.id(), participant);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();

        verify(eventPublisher, times(1)).send(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).containsExactly(
            new BookingCancelled(session.id(), participant),
            new SubscriptionCredited(subscription.id(), participant));

        verify(sessionRepository, times(1)).persist(persistedSessionCaptor.capture());
        assertThat(persistedSessionCaptor.getValue().id()).isEqualTo(session.id());
    }

    private Session sessionTest(int min, int max) {
        return sessionTest(min, max, null, null);
    }

    private Session sessionTest(int min, int max, ParticipantId withParticipant, SubscriptionId withSubscription) {
        var id = new SessionId(UUID.randomUUID());
        return new Session(
                id,
                new ArticleId(UUID.randomUUID()),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(min, max),
                new HashSet<>(withParticipant != null ? Set.of(bookingTest(id, withParticipant, withSubscription)) : Collections.emptySet()));
    }

    private Subscription subscriptionTest() {
        var today = LocalDate.now();
        return new Subscription(
                new SubscriptionId(UUID.randomUUID()),
                new ArticleId(UUID.randomUUID()),
                new Subscription.Validity(today, today.plusMonths(12)),
                null);
    }

    private Booking bookingTest(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Booking.Status.DONE);
    }

    private ParticipantId participantTest() {
        return new ParticipantId(UUID.randomUUID());
    }
}
