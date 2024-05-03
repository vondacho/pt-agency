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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionTest {
    @Test
    void sessionLifecycle() {
        var session = sessionTest(2, 2);
        var participant1 = participantTest();
        var subscription = subscriptionTest();

        var booking = session.book(participant1, subscription);

        assertThat(session.bookings()).hasSize(1);
        assertThat(session.bookings()).allMatch(it -> Booking.Status.PREBOOKED == it.status() && it.id().participant().equals(participant1));
        assertThat(session.domainEvents()).isEmpty();
        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant1);
        assertThat(booking.subscription()).isEqualTo(subscription);

        assertThat(session.findBooking(participant1)).isPresent();

        var participant2 = participantTest();
        booking = session.book(participant2, subscription);

        assertThat(session.bookings()).hasSize(2);
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.DONE == it.status() && it.id().participant().equals(participant1));
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.DONE == it.status() && it.id().participant().equals(participant2));
        var events = List.copyOf(session.domainEvents());
        assertThat(events).containsOnly(
                new SessionBooked(session.id(), participant1),
                new SessionBooked(session.id(), participant2),
                new SubscriptionCharged(subscription, participant1),
                new SubscriptionCharged(subscription, participant2));
        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant2);
        assertThat(booking.subscription()).isEqualTo(subscription);

        var participant3 = participantTest();
        booking = session.book(participant3, subscription);

        assertThat(session.bookings()).hasSize(3);
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.DONE == it.status() && it.id().participant().equals(participant1));
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.DONE == it.status() && it.id().participant().equals(participant2));
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.WAITING_LIST == it.status() && it.id().participant().equals(participant3));
        events = List.copyOf(session.domainEvents());
        assertThat(events).containsOnly(new ParticipantWaitlisted(session.id(), participant3));
        assertThat(booking.id().session()).isEqualTo(session.id());
        assertThat(booking.id().participant()).isEqualTo(participant3);
        assertThat(booking.subscription()).isEqualTo(subscription);

        session.cancel(participant1);

        assertThat(session.bookings()).hasSize(2);
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.DONE == it.status() && it.id().participant().equals(participant2));
        assertThat(session.bookings()).anyMatch(it -> Booking.Status.DONE == it.status() && it.id().participant().equals(participant3));
        events = List.copyOf(session.domainEvents());
        assertThat(events).containsOnly(
                new SessionBooked(session.id(), participant3),
                new SubscriptionCharged(subscription, participant3),
                new BookingCancelled(session.id(), participant1),
                new SubscriptionCredited(subscription, participant1));
    }

    private Session sessionTest(int min, int max) {
        return new Session(
                new SessionId(UUID.randomUUID()),
                new ArticleId(UUID.randomUUID()),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(min, max),
                new HashSet<>());
    }

    private ParticipantId participantTest() {
        return new ParticipantId(UUID.randomUUID());
    }
    private SubscriptionId subscriptionTest() {
        return new SubscriptionId(UUID.randomUUID());
    }
}
