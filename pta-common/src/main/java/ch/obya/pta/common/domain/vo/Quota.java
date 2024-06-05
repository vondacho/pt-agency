package ch.obya.pta.common.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;
import lombok.Builder;

import static ch.obya.pta.common.domain.util.CommonProblem.ifThrow;

@Builder(builderClassName = "Builder", toBuilder = true)
public record Quota(Integer min, Integer max) {
    public Quota {
        ifThrow(() -> min == null && max == null, CommonProblem.QuotaInvalid.toException(min, max));
        ifThrow(() -> ((min != null && min < 0) || (max != null && (max < 0 || (min != null && min > max)))), CommonProblem.QuotaInvalid.toException(min, max));
    }

    public boolean inQuota(Integer count) {
        return (min == null || min <= count) && (max == null || max >= count);
    }

    public boolean lowerThanMinQuota(Integer count) {
        return min > count;
    }

    public boolean moreThanMaxQuota(Integer count) {
        return max != null && count > max;
    }

    public static Quota unlimited() {
        return Quota.builder().min(0).max(null).build();
    }

    public static Quota limited(int max) {
        return Quota.builder().min(0).max(max).build();
    }

    public static Quota of(Integer min, Integer max) {
        return Quota.builder().min(min).max(max).build();
    }

    public Quota consume(int count) {
        if (max != null) {
            ifThrow(() -> count > max, CommonProblem.QuotaInvalid.toException(min, max - count));
            return Quota.of(min, max - count);
        }
        return this;
    }
}
