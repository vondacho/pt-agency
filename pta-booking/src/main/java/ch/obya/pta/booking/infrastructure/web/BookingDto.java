package ch.obya.pta.booking.infrastructure.web;


import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;

/**
 * @param status DONE (seat booked), PREBOOKED (seat held until the session's minimum is reached), WAITING (waiting list)
 */
public record BookingDto(SessionId session, ParticipantId participant, Booking.Status status) {

    public static BookingDto from(Booking booking) {
        return new BookingDto(booking.id().session(), booking.id().participant(), booking.status());
    }
}
