package ch.obya.pta.booking.domain;

import java.time.LocalDate;
import java.util.Objects;

public record Subscription(SubscriptionId id,
                           ArticleId articleId,
                           Validity validity,
                           Quota quota) {
    public record Validity(LocalDate from, LocalDate to) {
        public Validity {
            Objects.requireNonNull(from);
            if (to != null && from.isAfter(to))
                throw BookingProblem.SubscriptionValidityInvalid.toException(from, to);
        }

        public boolean includes(LocalDate date) {
            return !(date.isBefore(from) || (to == null || date.isAfter(to)));
        }
    }

    public Subscription {
        Objects.requireNonNull(id);
        Objects.requireNonNull(articleId);
        Objects.requireNonNull(validity);
    }
}
