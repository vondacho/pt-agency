package ch.obya.pta.booking.application;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.event.SessionRemoved;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.vo.*;
import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.util.EntityFinder;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class BookingService {

    private final SessionRepository sessionRepository;

    private final ArticleStore articleStore;

    private final CustomerProfile clientProfile;

    private final EventPublisher eventPublisher;

    @Transactional
    public Uni<BookingId> book(SessionId session, ParticipantId participant) {
        return findOne(session)
                .flatMap(it -> findOneSubscription(it, participant).map(su -> Tuple2.of(it, su)))
                .flatMap(it -> makeBooking(it.getItem1(), participant, it.getItem2()).map(bo -> Tuple2.of(it.getItem1(), bo)))
                .flatMap(it -> notify(it.getItem1().domainEvents()).replaceWith(it.getItem2()))
                .map(Booking::id);
    }

    @Transactional
    public Uni<Void> cancel(SessionId session, ParticipantId participant) {
        return findOne(session)
                .flatMap(it -> cancelBooking(it, participant).replaceWith(it))
                .flatMap(it -> notify(it.domainEvents()));
    }

    public Uni<Session> findOne(SessionId id) {
        return EntityFinder.find(Session.class, id, sessionRepository::findOne, CommonProblem.EntityNotFound);
    }

    @Transactional
    public Uni<Void> remove(SessionId session) {
        return findOne(session)
                .flatMap(c -> sessionRepository.remove(session))
                .invoke(() -> eventPublisher.publish(Set.of(new SessionRemoved(session))));
    }

    private Uni<SubscriptionId> findOneSubscription(Session session, ParticipantId participant) {
        return session.findBooking(participant)
                .map(it -> Uni.createFrom().item(it.subscription()))
                .orElseGet(() -> activeSubscriptionsOf(participant, session.state().slot().dow())
                    .collect().asMap(Subscription::articleId, it -> it)
                    .onItem().transformToMulti(it -> eligibleSubscriptions(it, session))
                    .select().first().toUni().map(Subscription::id)
                    .ifNoItem().after(Duration.ofMillis(100))
                    .failWith(BookingProblem.NoActiveSubscription.toException(participant, session.id(), session.articleId())));
    }

    private Multi<Subscription> activeSubscriptionsOf(ParticipantId participant, LocalDate sessionDate) {
        return clientProfile.validSubscriptionsOf(participant, sessionDate);
    }

    private Multi<Subscription> eligibleSubscriptions(Map<ArticleId, Subscription> subscriptions , Session session) {
        return articleStore.eligibleSubscriptionsFor(session.articleId())
                .map(subscriptions::get)
                .select().where(Objects::nonNull);
    }

    private Uni<Booking> makeBooking(Session session, ParticipantId participant, SubscriptionId subscriptionId) {
        return Uni.createFrom()
                .item(session.book(participant, subscriptionId))
                .invoke(() -> sessionRepository.save(session));
    }

    private Uni<Void> cancelBooking(Session session, ParticipantId participant) {
        return Uni.createFrom().voidItem()
                .invoke(() -> session.cancel(participant))
                .invoke(() -> sessionRepository.save(session));
    }

    private Uni<Void> notify(Collection<Event> events) {
        return eventPublisher.publish(events);
    }
}
