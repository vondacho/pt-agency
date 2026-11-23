package ch.obya.pta.www.booking;

import java.util.UUID;

/**
 * A booking as answered by pta-booking.
 *
 * @param status DONE (seat booked), PREBOOKED (held until the session's minimum is reached), WAITING (waiting list)
 */
public record BookingView(Ref session, Ref participant, String status) {

    public record Ref(UUID id) {
    }

    /** What the SPA needs: the session and the resulting status. */
    public record Result(UUID session, String status) {
    }

    public Result toResult() {
        return new Result(session.id(), status);
    }
}
