package ch.obya.pta.booking.domain;


import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.event.*;
import ch.obya.pta.booking.domain.vo.*;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.booking.domain.util.Samples;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

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
}
