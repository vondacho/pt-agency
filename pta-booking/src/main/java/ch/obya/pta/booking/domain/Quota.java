package ch.obya.pta.booking.domain;

import java.util.Objects;

public record Quota(Integer min, Integer max) {
    public Quota {
        Objects.requireNonNull(min);
        if (min < 0 || (max != null && (max < 0 || min > max))) {
            throw BookingProblem.QuotaInvalid.toException(min, max);
        }
    }

    public boolean contains(Integer count) {
        return min <= count && (max == null || max >= count);
    }

    public boolean above(Integer count) {
        return min > count;
    }

    public boolean exceededBy(Integer count) {
        return max != null && count > max;
    }
}
