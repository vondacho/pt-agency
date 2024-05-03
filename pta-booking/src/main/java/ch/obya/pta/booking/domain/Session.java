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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static ch.obya.pta.booking.domain.Booking.Status.WAITING_LIST;

@Accessors(fluent = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class Session {
    SessionId id;
    ArticleId articleId;
    String title;
    TimeSlot slot;
    Location location;
    Quota quota;
    Set<Booking> bookings;
    List<DomainEvent> domainEvents = new ArrayList<>();

    public Session(SessionId id, ArticleId articleId, String title, TimeSlot slot, Location location, Quota quota, Set<Booking> bookings) {
        this.id = id;
        this.articleId = articleId;
        this.title = title;
        this.slot = slot;
        this.location = location;
        this.quota = quota;
        this.bookings = bookings;
    }

    public record TimeSlot(LocalDate dow, LocalTime start, LocalTime stop, Duration duration) {
        public TimeSlot {
            Objects.requireNonNull(start);
            Objects.requireNonNull(stop);
            Objects.requireNonNull(duration);
            if (start.isAfter(stop)) {
                throw BookingProblem.WeekTimeSlotInvalid.toException(dow, start, stop, duration);
            }
        }
    }

    public Set<Booking> bookings() {
        return Set.copyOf(bookings);
    }

    public List<DomainEvent> domainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public Booking book(ParticipantId participant, SubscriptionId subscription) {
        var existingBooking = findBooking(participant);

        Booking booking;
        if (existingBooking.isPresent()) {
            bookings.removeIf(b -> b.id().equals(existingBooking.get().id()));
            booking = existingBooking.get().confirmed();
            andThen(new SessionBooked(this.id, participant),
                    new SubscriptionCharged(subscription, participant));
        }
        else {
            if (quota.contains(bookings.size())) {
                if (quota.exceededBy(bookings.size() + 1)) {
                    booking = Booking.waiting(id, participant, subscription);
                    andThen(new ParticipantWaitlisted(this.id, participant));
                } else {
                    booking = Booking.done(id, participant, subscription);
                    andThen(new SessionBooked(this.id, participant),
                            new SubscriptionCharged(subscription, participant));
                }
            } else if (quota.contains(bookings.size() + 1)) {
                var confirmedBookings = bookings.stream().map(Booking::confirmed).toList();
                bookings.clear();
                bookings.addAll(confirmedBookings);
                booking = Booking.done(id, participant, subscription);
                confirmedBookings.forEach(it ->
                    andThen(new SessionBooked(this.id, it.id().participant()),
                            new SubscriptionCharged(it.subscription(), it.id().participant()))
                );
                andThen(new SessionBooked(this.id, participant),
                        new SubscriptionCharged(subscription, participant));
            } else {
                booking = Booking.prebooked(id, participant, subscription);
            }
        }
        bookings.add(booking);
        return booking;
    }

    public void cancel(ParticipantId participant) {
        var booking = findBooking(participant);
        if (booking.isEmpty()) {
            throw BookingProblem.NoBooking.toException(this.id, participant);
        }
        bookings.removeIf(b -> b.id().equals(booking.get().id()));
        andThen(new BookingCancelled(this.id, participant),
                new SubscriptionCredited(booking.get().subscription(), participant));

        promoteOneWaitingParticipant();
    }

    private void promoteOneWaitingParticipant() {
        bookings.stream().filter(it -> WAITING_LIST == it.status()).findFirst().ifPresent(booking -> {
            bookings.removeIf(b -> b.id().equals(booking.id()));
            bookings.add(Booking.done(this.id, booking.id().participant(), booking.subscription()));
            andThen(new SessionBooked(this.id, booking.id().participant()),
                    new SubscriptionCharged(booking.subscription(), booking.id().participant()));
        });
    }

    public Optional<Booking> findBooking(ParticipantId participant) {
        return bookings.stream().filter(it -> it.id().equals(this.id, participant)).findFirst();
    }

    private void andThen(DomainEvent...events) {
        andThen(Arrays.stream(events).toList());
    }

    private void andThen(Collection<DomainEvent> events) {
        domainEvents.addAll(events);
    }
}
