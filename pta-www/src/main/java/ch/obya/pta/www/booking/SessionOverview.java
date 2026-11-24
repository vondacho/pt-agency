package ch.obya.pta.www.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Everything a participant needs to know about a session before booking it.
 *
 * @param course            what the course is about; absent when the catalogue is unavailable
 * @param cancellationHours a booked seat can be cancelled until this many hours before the start (absent: until the start)
 * @param cancellableUntil  until when a booked seat can be cancelled, when a deadline applies
 * @param cancellationOpen  false once the deadline has passed
 * @param access            subscriptions and passes giving access: those on sale, and those the participant holds
 * @param youHoldOne        whether the signed-in participant holds one valid on the session's day (absent when not signed in)
 * @param incomplete        true when a part could not be fetched (catalogue or customer unavailable)
 */
public record SessionOverview(SessionView session, Course course, BookingClient.Instructor instructor, BookingClient.Venue venue,
                              String note, Integer cancellationHours, LocalDateTime cancellableUntil, boolean cancellationOpen,
                              List<Access> access, Boolean youHoldOne, boolean incomplete) {

    public record Course(List<String> objectives, String level, String prerequisites, String whatToBring) {
    }

    /**
     * @param onSale it can be requested in the shop
     * @param held   the signed-in participant holds it, valid on the session's day
     */
    public record Access(UUID id, String name, boolean onSale, boolean held) {
    }
}
