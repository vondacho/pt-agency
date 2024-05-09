package ch.obya.pta.booking.domain;

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
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.event.*;
import ch.obya.pta.booking.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SessionTest {
    @Test
    void sessionLifecycle() {
        var session = sessionTest(1, 1);

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

        var participant1 = participantTest();
        var subscription = subscriptionTest();

        var booking = session.book(participant1, subscription);

        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant1);
        assertThat(booking.subscription()).isEqualTo(subscription);
        assertThat(session.findBooking(participant1)).isPresent();
        assertThat(session.bookings())
                .extracting(Booking::status, it -> it.id().participant())
                .containsExactly(tuple(Booking.Status.PREBOOKED, participant1));

        assertThat(session.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new SessionPrebooked(session.id(), participant1));
        assertThat(session.domainEvents()).isEmpty();

        var participant2 = participantTest();
        booking = session.book(participant2, subscription);

        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant2);
        assertThat(booking.subscription()).isEqualTo(subscription);
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
                    new SubscriptionCharged(subscription, participant1),
                    new SubscriptionCharged(subscription, participant2));
        assertThat(session.domainEvents()).isEmpty();

        var participant3 = participantTest();
        booking = session.book(participant3, subscription);

        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant3);
        assertThat(booking.subscription()).isEqualTo(subscription);
        assertThat(session.bookings())
                .extracting(Booking::status, it -> it.id().participant())
                .containsExactlyInAnyOrder(
                        tuple(Booking.Status.DONE, participant1),
                        tuple(Booking.Status.DONE, participant2),
                        tuple(Booking.Status.WAITING_LIST, participant3));
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
                        new SubscriptionCharged(subscription, participant3),
                        new BookingCancelled(session.id(), participant1),
                        new SubscriptionCredited(subscription, participant1));
        assertThat(session.domainEvents()).isEmpty();
    }

    private Session sessionTest(int min, int max) {
        return new Session(
                SessionId.create(),
                ArticleId.create(),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(min, max),
                new HashSet<>());
    }

    private ParticipantId participantTest() {
        return ParticipantId.create();
    }
    private SubscriptionId subscriptionTest() {
        return SubscriptionId.create();
    }
}
