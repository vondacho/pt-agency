package ch.obya.pta.booking.domain;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.event.*;
import ch.obya.pta.booking.domain.util.Samples;
import ch.obya.pta.booking.domain.vo.*;
import ch.obya.pta.common.domain.vo.Quota;

import ch.obya.pta.booking.domain.aggregate.Session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SessionTest {
    @Test
    void sessionLifecycle() {
        var session = Samples.onePrivateSession.get();

        session.modify()
                .rename("modified")
                .relocate(new Location("modified"))
                .reschedule(session.state().slot().toBuilder()
                        .stop(LocalTime.of(11, 0))
                        .duration(Duration.ofHours(2))
                        .build())
                .resize(new Quota(2, 2))
                .done();

        var state = session.state();

        assertThat(state.title()).isEqualTo("modified");
        assertThat(state.location().name()).isEqualTo("modified");
        assertThat(state.slot().start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(state.slot().stop()).isEqualTo(LocalTime.of(11, 0));
        assertThat(state.slot().duration()).isEqualTo(Duration.ofHours(2));
        assertThat(state.quota()).isEqualTo(new Quota(2, 2));

        var participant1 = Samples.oneParticipant.get();
        var subscription = Samples.oneYearlySubscription.get();

        var booking = session.book(participant1, subscription.id());

        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant1);
        assertThat(booking.subscription()).isEqualTo(subscription.id());
        assertThat(session.findBooking(participant1)).isPresent();
        assertThat(session.bookings())
                .extracting(Booking::status, it -> it.id().participant())
                .containsExactly(tuple(Booking.Status.PREBOOKED, participant1));

        assertThat(session.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new SessionPrebooked(session.id(), participant1));
        assertThat(session.domainEvents()).isEmpty();

        var participant2 = Samples.oneParticipant.get();
        booking = session.book(participant2, subscription.id());

        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant2);
        assertThat(booking.subscription()).isEqualTo(subscription.id());
        assertThat(session.bookings())
                .extracting(Booking::status, it -> it.id().participant())
                .containsExactlyInAnyOrder(
                        tuple(Booking.Status.DONE, participant1),
                        tuple(Booking.Status.DONE, participant2));
        assertThat(session.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactlyInAnyOrder(
                    new SessionBooked(session.id(), participant1),
                    new SessionBooked(session.id(), participant2),
                    new SubscriptionCharged(subscription.id(), participant1),
                    new SubscriptionCharged(subscription.id(), participant2));
        assertThat(session.domainEvents()).isEmpty();

        var participant3 = Samples.oneParticipant.get();
        booking = session.book(participant3, subscription.id());

        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant3);
        assertThat(booking.subscription()).isEqualTo(subscription.id());
        assertThat(session.bookings())
                .extracting(Booking::status, it -> it.id().participant())
                .containsExactlyInAnyOrder(
                        tuple(Booking.Status.DONE, participant1),
                        tuple(Booking.Status.DONE, participant2),
                        tuple(Booking.Status.WAITING, participant3));
        assertThat(session.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new ParticipantWaitlisted(session.id(), participant3));
        assertThat(session.domainEvents()).isEmpty();

        session.cancel(participant1);

        assertThat(session.bookings())
                .extracting(Booking::status, it -> it.id().participant())
                .containsExactlyInAnyOrder(
                        tuple(Booking.Status.DONE, participant2),
                        tuple(Booking.Status.DONE, participant3));
        assertThat(session.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactlyInAnyOrder(
                        new SessionBooked(session.id(), participant3),
                        new SubscriptionCharged(subscription.id(), participant3),
                        new BookingCancelled(session.id(), participant1),
                        new SubscriptionCredited(subscription.id(), participant1));
        assertThat(session.domainEvents()).isEmpty();
    }

    /** A session with {@code done} confirmed participants and {@code waiting} participants on its waiting list. */
    private static Session sessionWith(int min, int max, int done, int waiting) {
        var id = SessionId.create();
        var bookings = new java.util.HashSet<Booking>();
        for (int i = 0; i < done; i++) bookings.add(Booking.done(id, ParticipantId.create(), SubscriptionId.create()));
        for (int i = 0; i < waiting; i++) bookings.add(Booking.waiting(id, ParticipantId.create(), SubscriptionId.create()));
        var template = Samples.session(min, max, java.util.Set.of());
        return new Session(id, template.articleId(), "full", template.state().slot(), template.state().location(),
                new Quota(min, max), bookings);
    }

    private static long seated(Session session) {
        return session.bookings().stream().filter(b -> b.status() != Booking.Status.WAITING).count();
    }

    @Test
    void booking_a_full_session_with_a_waiting_list_joins_the_waiting_list() {
        // regression: waiting-list entries were counted as seats, so the next participant was pre-booked (6 of 5 seats)
        var session = sessionWith(2, 5, 5, 1);
        var participant = Samples.oneParticipant.get();

        var booking = session.book(participant, SubscriptionId.create());

        assertThat(booking.status()).isEqualTo(Booking.Status.WAITING);
        assertThat(seated(session)).isEqualTo(5);
        assertThat(session.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new ParticipantWaitlisted(session.id(), participant));
    }

    @Test
    void booking_the_last_seat_of_a_session_with_a_waiting_list_is_not_possible() {
        var session = sessionWith(1, 1, 1, 2);
        var booking = session.book(Samples.oneParticipant.get(), SubscriptionId.create());
        assertThat(booking.status()).isEqualTo(Booking.Status.WAITING);
        assertThat(seated(session)).isEqualTo(1);
    }

    @Test
    void booking_again_does_not_change_an_existing_booking() {
        // regression: booking again confirmed any existing booking, letting a waiting participant jump the queue
        var session = sessionWith(1, 1, 1, 0);
        var waiting = Samples.oneParticipant.get();
        session.book(waiting, SubscriptionId.create());
        session.domainEvents();

        var again = session.book(waiting, SubscriptionId.create());

        assertThat(again.status()).isEqualTo(Booking.Status.WAITING);
        assertThat(seated(session)).isEqualTo(1);
        assertThat(session.domainEvents()).isEmpty();
    }

    @Test
    void leaving_the_waiting_list_frees_no_seat() {
        // regression: any cancellation promoted a waiting participant, even when no seat had been freed
        var session = sessionWith(1, 1, 1, 0);
        var first = Samples.oneParticipant.get();
        var second = Samples.oneParticipant.get();
        session.book(first, SubscriptionId.create());
        session.book(second, SubscriptionId.create());
        session.domainEvents();

        session.cancel(second);

        assertThat(session.findBooking(first)).get().extracting(Booking::status).isEqualTo(Booking.Status.WAITING);
        assertThat(seated(session)).isEqualTo(1);
    }

    @Test
    void cancelling_a_seat_gives_it_to_the_first_waiting_participant() {
        var session = sessionWith(1, 1, 0, 0);
        var seated = Samples.oneParticipant.get();
        var waiting = Samples.oneParticipant.get();
        session.book(seated, SubscriptionId.create());
        session.book(waiting, SubscriptionId.create());

        session.cancel(seated);

        assertThat(session.findBooking(waiting)).get().extracting(Booking::status).isEqualTo(Booking.Status.DONE);
        assertThat(seated(session)).isEqualTo(1);
    }
}
