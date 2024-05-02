package ch.obya.pta.booking.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Accessors(fluent = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor
public class Booking {
    BookingId id;
    SubscriptionId subscription;
    Status status;

    public enum Status {
        DONE, WAITING_LIST, PREBOOKED
    }

    public static Booking done(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.DONE);
    }

    public Booking confirmed() {
        return done(id.session(), id.participant(), subscription);
    }

    public static Booking waiting(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.WAITING_LIST);
    }

    public static Booking prebooked(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.PREBOOKED);
    }
}
