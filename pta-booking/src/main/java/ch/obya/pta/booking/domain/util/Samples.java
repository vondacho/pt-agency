package ch.obya.pta.booking.domain.util;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.*;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Validity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Samples {

    public static Session session(int min, int max, Set<Booking> bookings) {
        return new Session(
                SessionId.create(),
                ArticleId.create(),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(min, max),
                bookings);
    }

    private static Subscription yearlySubscription() {
        var today = LocalDate.now();
        return new Subscription(
                SubscriptionId.create(),
                ArticleId.create(),
                new Validity(today, today.plusMonths(12)));
    }

    public static final Supplier<Session> onePrivateSession = () -> session(1,1, Set.of());
    public static final Supplier<Session> oneSmallGroupSession = () -> session(2,5, Set.of());
    public static final Supplier<Session> oneCollectiveCourseSession = () -> session(3,20, Set.of());
    public static final Supplier<ParticipantId> oneParticipant = ParticipantId::create;
    public static final Supplier<Subscription> oneYearlySubscription = Samples::yearlySubscription;
}
