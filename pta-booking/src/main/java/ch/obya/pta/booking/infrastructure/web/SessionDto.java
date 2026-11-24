package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.ParticipantId;

/**
 * Read model of a training session, as presented to participants.
 *
 * @param minSeats       number of bookings required for the session to take place (null: no minimum)
 * @param maxSeats       maximum number of bookings (null: unlimited)
 * @param bookedSeats    bookings holding a seat, i.e. done or pre-booked
 * @param availableSeats seats still free (null: unlimited)
 * @param waitlisted     participants on the waiting list
 * @param confirmed      true when the minimum number of bookings is reached
 * @param participantBooking status of the booking of the participant the list was requested for, if any
 * @param instructor     name of the instructor leading the session, if any
 */
public record SessionDto(
        UUID id,
        String title,
        LocalDate day,
        LocalTime start,
        LocalTime stop,
        String location,
        Integer minSeats,
        Integer maxSeats,
        int bookedSeats,
        Integer availableSeats,
        int waitlisted,
        boolean confirmed,
        Booking.Status participantBooking,
        String instructor) {

    public static SessionDto from(Session session) {
        return from(session, null, null);
    }

    public static SessionDto from(Session session, ParticipantId participant) {
        return from(session, participant, null);
    }

    public static SessionDto from(Session session, ParticipantId participant, String instructor) {
        var state = session.state();
        var quota = state.quota();
        var booked = (int) session.bookings().stream().filter(b -> b.status() != Booking.Status.WAITING).count();
        var waiting = (int) session.bookings().stream().filter(b -> b.status() == Booking.Status.WAITING).count();
        return new SessionDto(
                session.id().id(),
                state.title(),
                state.slot().dow(),
                state.slot().start(),
                state.slot().stop(),
                state.location().name(),
                quota.min(),
                quota.max(),
                booked,
                quota.max() == null ? null : Math.max(0, quota.max() - booked),
                waiting,
                quota.min() == null || booked >= quota.min(),
                participant == null ? null : session.findBooking(participant).map(Booking::status).orElse(null),
                instructor);
    }
}
