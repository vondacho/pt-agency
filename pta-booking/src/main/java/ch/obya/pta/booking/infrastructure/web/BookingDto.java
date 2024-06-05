package ch.obya.pta.booking.infrastructure.web;


import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;

public record BookingDto(SessionId session, ParticipantId participant) {

    public static BookingDto from(Booking booking) {
        return new BookingDto(booking.id().session(), booking.id().participant());
    }
}
