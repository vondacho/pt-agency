package ch.obya.pta.booking.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum BookingProblem {

    NoSession("Session %s does not exist."),
    SessionQuotaReached("Session %s: no more participant allowed, the quota (%d) is reached."),
    SubscriptionQuotaReached("Subscription %s: no more session allowed, the quota (%d) is reached."),
    NoActiveSubscription("No active subscription of participant %s applies to session %s (article %s)."),
    SubscriptionStillNotActive("Subscription %s is still not active (%s < %s)"),
    SubscriptionNoMoreActive("Subscription %s is no more active (%s > %s)."),
    SubscriptionValidityInvalid("Subscription validity (%s, %s) is not valid."),
    NoBooking("Booking on session %s for participant %s does not exist."),
    NoSessionForBooking("Session with booking %s does not exist."),
    QuotaInvalid("Quota (%s, %s) is not valid."),
    WeekTimeSlotInvalid("Week time slot (%s, %s, %s, %s) is not valid.");

    private final String template;

    @Getter
    @Accessors(fluent = true)
    @FieldDefaults(makeFinal=true)
    public static class Exception extends RuntimeException {
        String code;
        Object[] parameters;

        private Exception(String code, String template, Object...parameters) {
            super(template.formatted(parameters));
            this.code = code;
            this.parameters = parameters;
        }
    }

    public Exception toException(Object...parameters) {
        return new Exception(this.name(), template, parameters);
    }
}
