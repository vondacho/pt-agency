package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Participant-facing view of a training session, as published by pta-booking.
 * pta-www keeps its own copy of the contract so that the SPA is shielded from booking internals.
 *
 * @param minSeats       bookings required for the session to take place (null: no minimum)
 * @param maxSeats       seats offered (null: unlimited)
 * @param bookedSeats    seats taken
 * @param availableSeats seats still free (null: unlimited)
 * @param waitlisted     participants on the waiting list
 * @param confirmed      true when the minimum number of bookings is reached
 * @param participantBooking status of the signed-in participant's booking (DONE, PREBOOKED, WAITING), if any
 * @param instructor     name of the instructor leading the session, if any
 */
public record SessionView(
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
        String participantBooking,
        String instructor) {
}
