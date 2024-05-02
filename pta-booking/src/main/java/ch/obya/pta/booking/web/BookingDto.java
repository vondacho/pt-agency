package ch.obya.pta.booking.web;

import ch.obya.pta.booking.domain.Booking;

import java.util.UUID;

public record BookingDto(UUID session, UUID participant) {

    public static BookingDto from(Booking booking) {
        return new BookingDto(booking.id().session().id(), booking.id().participant().id());
    }
}
