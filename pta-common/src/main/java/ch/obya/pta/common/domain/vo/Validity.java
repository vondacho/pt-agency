package ch.obya.pta.common.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;

import java.time.LocalDate;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifThrow;

public record Validity(LocalDate from, LocalDate to) {
    public Validity {
        ifNullThrow(from, CommonProblem.AttributeNotNull.toException("Validity.from"));
        ifThrow(() -> to != null && from.isAfter(to), CommonProblem.ValidityInvalid.toException(from, to));
    }

    /** True when the date is within the validity; an open-ended validity ({@code to == null}) never ends. */
    public boolean includes(LocalDate date) {
        return !date.isBefore(from) && (to == null || !date.isAfter(to));
    }

    /** True when both validities share at least one day (open-ended ones never end). */
    public boolean overlappedBy(Validity other) {
        return (other.to == null || !other.to.isBefore(from)) && (to == null || !to.isBefore(other.from));
    }

    public Validity closeOn(LocalDate date) {
        return new Validity(from, date);
    }

    public static Validity openOn(LocalDate date) {
        return new Validity(date, null);
    }

    public static Validity fromNow() {
        return new Validity(LocalDate.now(), null);
    }

    public Validity closeNow() {
        return closeOn(LocalDate.now());
    }
}

