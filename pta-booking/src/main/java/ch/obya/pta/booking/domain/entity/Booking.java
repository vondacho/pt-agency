package ch.obya.pta.booking.domain.entity;

import ch.obya.pta.booking.domain.vo.BookingId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.booking.domain.vo.SubscriptionId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Accessors(fluent = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Booking {
    BookingId id;
    SubscriptionId subscription;
    Status status;

    public enum Status {
        DONE, WAITING, PREBOOKED
    }

    public static Booking done(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.DONE);
    }

    public Booking confirmed() {
        return done(id.session(), id.participant(), subscription);
    }

    public static Booking waiting(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.WAITING);
    }

    public static Booking prebooked(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.PREBOOKED);
    }
}
