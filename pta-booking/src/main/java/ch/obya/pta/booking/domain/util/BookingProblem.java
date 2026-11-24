package ch.obya.pta.booking.domain.util;

import ch.obya.pta.common.domain.util.Problem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum BookingProblem implements Problem {

    NoSession("Session %s does not exist."),
    SessionQuotaReached("Session %s: no more participant allowed, the quota (%d) is reached."),
    SubscriptionQuotaReached("Subscription %s: no more session allowed, the quota (%d) is reached."),
    NoActiveSubscription("No active subscription of participant %s applies to session %s (article %s)."),
    SubscriptionStillNotActive("Subscription %s is still not active (%s < %s)"),
    SubscriptionNoMoreActive("Subscription %s is no more active (%s > %s)."),
    NoBooking("Booking on session %s for participant %s does not exist."),
    NoSessionForBooking("Session with booking %s does not exist."),
    TimeSlotInvalid("Week time slot (%s, %s, %s, %s) is not valid."),
    ServiceUnavailable("Service %s is unavailable: %s"),
    ConcurrentModification("Session %s was changed by another booking, please retry."),
    CancellationClosed("Session %s: bookings can no longer be cancelled (%s hours before the start).");

    private final String text;
}
