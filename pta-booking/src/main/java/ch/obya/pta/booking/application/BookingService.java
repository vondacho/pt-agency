package ch.obya.pta.booking.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
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

/**
 * Booking use cases. Each one runs in a single transaction, so that a session changed concurrently between
 * its load and its save is detected (optimistic locking) instead of being silently overwritten.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class BookingService {

    private final SessionRepository sessionRepository;

    private final ArticleStore articleStore;

    private final CustomerProfile clientProfile;

    private final EventPublisher eventPublisher;

    /**
     * Books a seat for the participant, or puts them on the waiting list when the session is full.
     * Requires an active subscription of the participant which gives access to the session's article.
     */
    @WithTransaction
    public Uni<Booking> book(SessionId session, ParticipantId participant) {
        return findOne(session)
                .flatMap(it -> findOneSubscription(it, participant).map(su -> Tuple2.of(it, su)))
                .flatMap(it -> makeBooking(it.getItem1(), participant, it.getItem2()).map(bo -> Tuple2.of(it.getItem1(), bo)))
                .flatMap(it -> notify(it.getItem1().domainEvents()).replaceWith(it.getItem2()));
    }

    @WithTransaction
    public Uni<Void> cancel(SessionId session, ParticipantId participant) {
        return findOne(session)
                .flatMap(it -> cancelBooking(it, participant).replaceWith(it))
                .flatMap(it -> notify(it.domainEvents()));
    }

    @WithTransaction
    public Uni<Session> findOne(SessionId id) {
        return EntityFinder.find(Session.class, id, sessionRepository::findOne, CommonProblem.EntityNotFound);
    }

    @WithTransaction
    public Uni<Void> remove(SessionId session) {
        return findOne(session)
                .flatMap(c -> sessionRepository.remove(session))
                .call(() -> eventPublisher.publish(Set.of(new SessionRemoved(session))));
    }

    private Uni<SubscriptionId> findOneSubscription(Session session, ParticipantId participant) {
        return session.findBooking(participant)
                .map(it -> Uni.createFrom().item(it.subscription()))
                .orElseGet(() -> activeSubscriptionsOf(participant, session.state().slot().dow())
                        .collect().asMap(Subscription::articleId, it -> it)
                        .onItem().transformToMulti(it -> eligibleSubscriptions(it, session))
                        .select().first().toUni()
                        // no eligible subscription: an empty result, not a timeout, so that slow services are not misreported
                        .onItem().ifNull().failWith(BookingProblem.NoActiveSubscription.toException(participant, session.id(), session.articleId()))
                        .map(Subscription::id));
    }

    private Multi<Subscription> activeSubscriptionsOf(ParticipantId participant, LocalDate sessionDate) {
        return clientProfile.validSubscriptionsOf(participant, sessionDate);
    }

    private Multi<Subscription> eligibleSubscriptions(Map<ArticleId, Subscription> subscriptions, Session session) {
        // keep the eligible articles the participant holds a subscription to (Mutiny items can't be null)
        return articleStore.eligibleSubscriptionsFor(session.articleId())
                .select().where(subscriptions::containsKey)
                .map(subscriptions::get);
    }

    private Uni<Booking> makeBooking(Session session, ParticipantId participant, SubscriptionId subscriptionId) {
        return Uni.createFrom()
                .item(() -> session.book(participant, subscriptionId))
                .call(() -> sessionRepository.save(session));
    }

    private Uni<Void> cancelBooking(Session session, ParticipantId participant) {
        return Uni.createFrom().voidItem()
                .invoke(() -> session.cancel(participant))
                .call(() -> sessionRepository.save(session))
                .replaceWithVoid();
    }

    private Uni<Void> notify(Collection<Event> events) {
        return eventPublisher.publish(events);
    }
}
