package ch.obya.pta.booking.domain.aggregate;


import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.event.*;
import ch.obya.pta.booking.domain.vo.*;
import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.util.CommonProblem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static ch.obya.pta.booking.domain.entity.Booking.Status.WAITING;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifThrow;

@Accessors(chain = true, fluent = true)
@Getter
public class Session extends BaseEntity<Session, SessionId, Session.State> {

    private final ArticleId articleId;
    @lombok.Builder(builderClassName = "Builder", toBuilder = true, access = AccessLevel.PRIVATE)
    public record State(
        String title,
        TimeSlot slot,
        Location location,
        Quota quota,
        Set<Booking> bookings,
        /* optional: who leads the session, where it takes place, a note for this session */
        InstructorId instructor,
        VenueId venue,
        String note) {
        public State {
            ifNullThrow(title, CommonProblem.AttributeNotNull.toException("Session.title"));
            ifNullThrow(slot, CommonProblem.AttributeNotNull.toException("Session.slot"));
            ifNullThrow(location, CommonProblem.AttributeNotNull.toException("Session.location"));
            ifNullThrow(quota, CommonProblem.AttributeNotNull.toException("Session.quota"));
        }
    }

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public record TimeSlot(LocalDate dow, LocalTime start, LocalTime stop, Duration duration) {
        public TimeSlot {
            ifNullThrow(start, CommonProblem.AttributeNotNull.toException("TimeSlot.start"));
            ifNullThrow(stop, CommonProblem.AttributeNotNull.toException("TimeSlot.stop"));
            ifNullThrow(duration, CommonProblem.AttributeNotNull.toException("TimeSlot.duration"));
            ifThrow(() -> start.isAfter(stop), BookingProblem.TimeSlotInvalid.toException(dow, start, stop, duration));
        }
    }

    public Session(SessionId id, ArticleId articleId, String title, TimeSlot slot, Location location, Quota quota, Set<Booking> bookings) {
        super(id, new State(title, slot, location, quota, new HashSet<>(bookings), null, null, null));
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

    /**
     * Books a seat for the participant: confirmed once the session's minimum is reached (the booking that reaches it
     * confirms the pre-booked ones), pre-booked below it, on the waiting list when no seat is left.
     * Booking again leaves an existing booking unchanged.
     */
    public Booking book(ParticipantId participant, SubscriptionId subscription) {
        var existingBooking = findBooking(participant);
        if (existingBooking.isPresent()) {
            return existingBooking.get();
        }

        // participants on the waiting list hold no seat
        var seated = (int) state.bookings.stream().filter(b -> b.status() != WAITING).count();

        Booking booking;
        if (state.quota.inQuota(seated)) {
            if (state.quota.moreThanMaxQuota(seated + 1)) {
                booking = Booking.waiting(id, participant, subscription);
                andEvent(new ParticipantWaitlisted(this.id, participant));
            } else {
                booking = Booking.done(id, participant, subscription);
                andEvent(new SessionBooked(this.id, participant),
                        new SubscriptionCharged(subscription, participant));
            }
        } else if (state.quota.inQuota(seated + 1)) {
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
        state.bookings.add(booking);
        return booking;
    }

    /** When the session starts. */
    public LocalDateTime startsAt() {
        return LocalDateTime.of(state.slot.dow(), state.slot.start());
    }

    /**
     * Until when a booked seat can be cancelled, given the course's deadline in hours; absent when there is none.
     */
    public Optional<LocalDateTime> cancellableUntil(Integer cancellationHours) {
        return Optional.ofNullable(cancellationHours).map(hours -> startsAt().minusHours(hours));
    }

    public void cancel(ParticipantId participant) {
        cancel(participant, null, null);
    }

    /**
     * Cancels the participant's booking. A booked seat can only be cancelled until the course's deadline
     * ({@code cancellationHours} before the start); leaving the waiting list or a pre-booking is always possible,
     * the session not being confirmed for the participant yet.
     */
    public void cancel(ParticipantId participant, LocalDateTime now, Integer cancellationHours) {
        var booking = findBooking(participant);
        ifThrow(booking::isEmpty, BookingProblem.NoBooking.toException(this.id, participant));
        ifThrow(() -> booking.get().status() == Booking.Status.DONE && now != null
                        && cancellableUntil(cancellationHours).map(deadline -> !now.isBefore(deadline)).orElse(false),
                BookingProblem.CancellationClosed.toException(this.id, cancellationHours));

        state.bookings.removeIf(b -> b.id().equals(booking.get().id()));
        andEvent(new BookingCancelled(this.id, participant),
                new SubscriptionCredited(booking.get().subscription(), participant));

        // only a cancelled seat can go to the waiting list; leaving the waiting list frees none
        if (booking.get().status() != WAITING) {
            promoteOneWaitingParticipant();
        }
    }

    private void promoteOneWaitingParticipant() {
        state.bookings.stream().filter(it -> WAITING == it.status()).findFirst().ifPresent(booking -> {
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

        public Modifier assign(InstructorId instructor) {
            stateBuilder.instructor(instructor);
            return this;
        }

        public Modifier host(VenueId venue) {
            stateBuilder.venue(venue);
            return this;
        }

        public Modifier annotate(String note) {
            stateBuilder.note(note);
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
