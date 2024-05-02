package ch.obya.pta.booking.application;

import ch.obya.pta.booking.domain.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class BookingService {

    private final SessionRepository sessionRepository;

    private final ArticleStore articleStore;

    private final ClientProfile clientProfile;

    private final EventPublisher eventPublisher;

    public Uni<BookingId> book(SessionId session, ParticipantId participant) {
        return findSession(session)
                .flatMap(it -> findOneSubscription(it, participant).map(su -> Tuple2.of(it, su)))
                .flatMap(it -> makeBooking(it.getItem1(), participant, it.getItem2()).map(bo -> Tuple2.of(it.getItem1(), bo)))
                .flatMap(it -> notify(it.getItem1().domainEvents()).replaceWith(it.getItem2()))
                .map(Booking::id);
    }

    public Uni<Void> cancel(SessionId session, ParticipantId participant) {
        return findSession(session)
                .flatMap(it -> cancelBooking(it, participant).replaceWith(it))
                .flatMap(it -> notify(it.domainEvents()));
    }

    private Uni<Session> findSession(SessionId session) {
        return sessionRepository.findById(session)
                .ifNoItem().after(Duration.ofMillis(100))
                .failWith(() -> BookingProblem.NoSession.toException(session));
    }

    private Uni<SubscriptionId> findOneSubscription(Session session, ParticipantId participant) {
        return session.findBooking(participant)
                .map(it -> Uni.createFrom().item(it.subscription()))
                .orElseGet(() ->
            findActiveSubscriptions(participant, session.slot().dow())
                .flatMap(candidates -> selectMatchingSubscriptions(candidates, session))
                .flatMap(this::selectOneSubscription)
                .map(Subscription::id)
                .ifNoItem().after(Duration.ofMillis(100))
                .failWith(() -> BookingProblem.NoActiveSubscription.toException(participant, session.id(), session.articleId())));
    }

    private Uni<List<Subscription>> findActiveSubscriptions(ParticipantId participant, LocalDate sessionDate) {
        return clientProfile.getSubscriptions(participant, sessionDate)
                .flatMap(it -> it.isEmpty() ? Uni.createFrom().nothing() : Uni.createFrom().item(it));
    }

    private Uni<List<Subscription>> selectMatchingSubscriptions(List<Subscription> candidates, Session session) {
        var candidatesByArticle = candidates.stream().collect(Collectors.toMap(Subscription::articleId, it -> it));
        return articleStore.selectMatchingSubscriptions(candidatesByArticle.keySet(), session.articleId())
                .flatMap(it -> it.isEmpty() ? Uni.createFrom().nothing() : Uni.createFrom().item(it))
                .map(matches -> matches.stream().map(candidatesByArticle::get).toList());
    }

    private Uni<Subscription> selectOneSubscription(List<Subscription> candidates) {
        return candidates.stream()
                .findFirst().map(c -> Uni.createFrom().item(c))
                .orElseGet(() -> Uni.createFrom().nothing());
    }

    private Uni<Booking> makeBooking(Session session, ParticipantId participant, SubscriptionId subscriptionId) {
        return Uni.createFrom()
                .item(session.book(participant, subscriptionId))
                .invoke(() -> sessionRepository.persist(session));
    }

    private Uni<Void> cancelBooking(Session session, ParticipantId participant) {
        return Uni.createFrom().voidItem()
                .invoke(() -> session.cancel(participant))
                .invoke(() -> sessionRepository.persist(session));
    }

    private Uni<Void> notify(Collection<DomainEvent> events) {
        return eventPublisher.send(events);
    }
}