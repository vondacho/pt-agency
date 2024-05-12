package ch.obya.pta.booking.domain.aggregate;

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

import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.event.*;
import ch.obya.pta.booking.domain.vo.*;
import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.util.CommonProblem;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static ch.obya.pta.booking.domain.entity.Booking.Status.WAITING_LIST;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifThrow;

@Accessors(chain = true, fluent = true)
@Getter
public class Session extends BaseEntity<Session, SessionId, Session.State> {

    private final ArticleId articleId;
    @Builder(builderClassName = "Builder", toBuilder = true, access = AccessLevel.PRIVATE)
    public record State(
        String title,
        TimeSlot slot,
        Location location,
        Quota quota,
        Set<Booking> bookings) {
        public State {
            ifNullThrow(title, CommonProblem.AttributeNotNull.toException("Session.title"));
            ifNullThrow(slot, CommonProblem.AttributeNotNull.toException("Session.slot"));
            ifNullThrow(location, CommonProblem.AttributeNotNull.toException("Session.location"));
            ifNullThrow(quota, CommonProblem.AttributeNotNull.toException("Session.quota"));
        }
    }

    @Builder(builderClassName = "Builder", toBuilder = true)
    public record TimeSlot(LocalDate dow, LocalTime start, LocalTime stop, Duration duration) {
        public TimeSlot {
            ifNullThrow(start, CommonProblem.AttributeNotNull.toException("TimeSlot.start"));
            ifNullThrow(stop, CommonProblem.AttributeNotNull.toException("TimeSlot.stop"));
            ifNullThrow(duration, CommonProblem.AttributeNotNull.toException("TimeSlot.duration"));
            ifThrow(() -> start.isAfter(stop), BookingProblem.TimeSlotInvalid.toException(dow, start, stop, duration));
        }
    }

    public Session(SessionId id, ArticleId articleId, String title, TimeSlot slot, Location location, Quota quota, Set<Booking> bookings) {
        super(id, new State(title, slot, location, quota, new HashSet<>(bookings)));
        ifNullThrow(articleId, CommonProblem.AttributeNotNull.toException("Session.articleId"));
        this.articleId = articleId;
    }

    public Session(ArticleId articleId, String title, TimeSlot slot, Location location, Quota quota) {
        this(SessionId.create(), articleId, title, slot, location, quota, new HashSet<>());
    }

    @Override
    protected State cloneState() {
        return state.toBuilder().build();
    }

    @Override
    public State validate(State state) {
        return state;
    }

    public Set<Booking> bookings() {
        return Set.copyOf(state.bookings);
    }

    public Booking book(ParticipantId participant, SubscriptionId subscription) {
        var existingBooking = findBooking(participant);

        Booking booking;
        if (existingBooking.isPresent()) {
            state.bookings.removeIf(b -> b.id().equals(existingBooking.get().id()));
            booking = existingBooking.get().confirmed();
            andEvent(new SessionBooked(this.id, participant),
                    new SubscriptionCharged(subscription, participant));
        } else {
            if (state.quota.inQuota(state.bookings.size())) {
                if (state.quota.moreThanMaxQuota(state.bookings.size() + 1)) {
                    booking = Booking.waiting(id, participant, subscription);
                    andEvent(new ParticipantWaitlisted(this.id, participant));
                } else {
                    booking = Booking.done(id, participant, subscription);
                    andEvent(new SessionBooked(this.id, participant),
                            new SubscriptionCharged(subscription, participant));
                }
            } else if (state.quota.inQuota(state.bookings.size() + 1)) {
                var confirmedBookings = state.bookings.stream().map(Booking::confirmed).toList();
                state.bookings.clear();
                state.bookings.addAll(confirmedBookings);
                booking = Booking.done(id, participant, subscription);
                confirmedBookings.forEach(it ->
                        andEvent(new SessionBooked(this.id, it.id().participant()),
                                new SubscriptionCharged(it.subscription(), it.id().participant()))
                );
                andEvent(new SessionBooked(this.id, participant),
                        new SubscriptionCharged(subscription, participant));
            } else {
                booking = Booking.prebooked(id, participant, subscription);
                andEvent(new SessionPrebooked(this.id, participant));
            }
        }
        state.bookings.add(booking);
        return booking;
    }

    public void cancel(ParticipantId participant) {
        var booking = findBooking(participant);
        ifThrow(booking::isEmpty, BookingProblem.NoBooking.toException(this.id, participant));

        state.bookings.removeIf(b -> b.id().equals(booking.get().id()));
        andEvent(new BookingCancelled(this.id, participant),
                new SubscriptionCredited(booking.get().subscription(), participant));

        promoteOneWaitingParticipant();
    }

    private void promoteOneWaitingParticipant() {
        state.bookings.stream().filter(it -> WAITING_LIST == it.status()).findFirst().ifPresent(booking -> {
            state.bookings.removeIf(b -> b.id().equals(booking.id()));
            state.bookings.add(Booking.done(this.id, booking.id().participant(), booking.subscription()));
            andEvent(new SessionBooked(this.id, booking.id().participant()),
                    new SubscriptionCharged(booking.subscription(), booking.id().participant()));
        });
    }

    public Optional<Booking> findBooking(ParticipantId participant) {
        return state.bookings.stream().filter(it -> it.id().equals(this.id, participant)).findFirst();
    }

    public Session.Modifier modify() {
        return this.new Modifier();
    }

    public class Modifier {
        private final State.Builder stateBuilder = state.toBuilder();

        public Modifier rename(String title) {
            stateBuilder.title(title);
            return this;
        }

        public Modifier relocate(Location location) {
            stateBuilder.location(location);
            return this;
        }

        public Modifier reschedule(TimeSlot slot) {
            stateBuilder.slot(slot);
            return this;
        }

        public Modifier resize(Quota quota) {
            stateBuilder.quota(quota);
            return this;
        }

        public void done() {
            state = stateBuilder.build();
        }
    }

}
