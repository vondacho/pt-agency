package ch.obya.pta.booking.infrastructure.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.StaleStateException;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.util.Samples;
import ch.obya.pta.booking.domain.vo.SessionId;

import static ch.obya.pta.common.util.search.AttributeFilter.equal;
import static ch.obya.pta.common.util.search.AttributeFilter.greaterThanOrEqual;
import static ch.obya.pta.common.util.search.AttributeFilter.lessThanOrEqual;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PanacheSessionRepositoryAdapterTest {

    @Inject
    PanacheSessionRepositoryAdapter repository;

    @RunOnVertxContext
    @Test
    void saved_session_with_bookings_should_be_found_with_the_same_state(UniAsserter asserter) {
        var session = Samples.oneSmallGroupSession.get();
        session.book(Samples.oneParticipant.get(), Samples.oneYearlySubscription.get().id());
        session.book(Samples.oneParticipant.get(), Samples.oneYearlySubscription.get().id());

        asserter.assertThat(
                () -> repository.save(session).flatMap(repository::findOne),
                found -> {
                    assertThat(found.id()).isEqualTo(session.id());
                    assertThat(found.articleId()).isEqualTo(session.articleId());
                    assertThat(found.state().title()).isEqualTo(session.state().title());
                    assertThat(found.state().slot()).isEqualTo(session.state().slot());
                    assertThat(found.state().quota()).isEqualTo(session.state().quota());
                    assertThat(found.bookings())
                            .extracting(Booking::id, Booking::subscription, Booking::status)
                            .containsExactlyInAnyOrderElementsOf(session.bookings().stream()
                                    .map(b -> org.assertj.core.groups.Tuple.tuple(b.id(), b.subscription(), b.status()))
                                    .toList());
                });
    }

    @RunOnVertxContext
    @Test
    void updated_session_should_drop_cancelled_bookings(UniAsserter asserter) {
        var session = Samples.oneSmallGroupSession.get();
        var participant = Samples.oneParticipant.get();
        session.book(participant, Samples.oneYearlySubscription.get().id());

        asserter.execute(() -> repository.save(session));
        asserter.execute(() -> session.cancel(participant));
        asserter.assertThat(
                () -> repository.save(session.id(), session.state()).flatMap(repository::findOne),
                found -> assertThat(found.bookings()).isEmpty());
    }

    @RunOnVertxContext
    @Test
    void sessions_should_be_found_by_day_range(UniAsserter asserter) {
        var tomorrow = LocalDate.now().plusDays(1);
        asserter.assertThat(
                () -> repository.findByCriteria(Map.of("day", List.of(greaterThanOrEqual(tomorrow), lessThanOrEqual(tomorrow)))),
                found -> assertThat(found)
                        .extracting(Session::id)
                        .containsExactly(
                                SessionId.parse("00000000-0000-0000-0000-000000000001"),
                                SessionId.parse("00000000-0000-0000-0000-000000000002")));
    }

    @RunOnVertxContext
    @Test
    void unknown_session_should_not_be_found(UniAsserter asserter) {
        asserter.assertThat(
                () -> repository.findOne(SessionId.create()),
                found -> assertThat(found).isNull());
        asserter.assertThat(
                () -> repository.findByCriteria(Map.of("id", List.of(equal(SessionId.create())))),
                found -> assertThat(found).isEmpty());
    }

    @RunOnVertxContext
    @Test
    void saving_a_session_again_should_update_its_bookings_in_place(UniAsserter asserter) {
        var session = Samples.oneSmallGroupSession.get();
        var first = Samples.oneParticipant.get();
        session.book(first, Samples.oneYearlySubscription.get().id());

        asserter.execute(() -> repository.save(session));
        // regression: re-saving used to re-insert the existing booking rows before deleting them
        asserter.execute(() -> session.book(Samples.oneParticipant.get(), Samples.oneYearlySubscription.get().id()));
        asserter.assertThat(
                () -> repository.save(session).flatMap(repository::findOne),
                found -> assertThat(found.bookings())
                        .extracting(Booking::id, Booking::status)
                        .containsExactlyInAnyOrderElementsOf(session.bookings().stream()
                                .map(b -> org.assertj.core.groups.Tuple.tuple(b.id(), b.status()))
                                .toList()));
    }

    @RunOnVertxContext
    @Test
    void session_changed_concurrently_should_not_be_overwritten(UniAsserter asserter) {
        var session = Samples.oneSmallGroupSession.get();
        asserter.execute(() -> repository.save(session));
        asserter.assertFailedWith(
                () -> Panache.withTransaction(() -> PanacheSessionEntity.findByLogicalId(session.id()).firstResult()
                        // another transaction commits a change of the same session meanwhile
                        .call(e -> Panache.getSession().flatMap(s -> s
                                .createNativeQuery("update training_session set version = version + 1 where logicalId = :id")
                                .setParameter("id", session.id().id())
                                .executeUpdate()))
                        .invoke(e -> e.title = "changed")),
                failure -> assertThat(failure).isInstanceOfAny(OptimisticLockException.class, StaleStateException.class));
    }

    @RunOnVertxContext
    @Test
    void removed_session_should_no_longer_be_found(UniAsserter asserter) {
        var session = Samples.onePrivateSession.get();
        asserter.execute(() -> repository.save(session));
        asserter.execute(() -> repository.remove(session.id()));
        asserter.assertThat(() -> repository.findOne(session.id()), found -> assertThat(found).isNull());
    }
}
