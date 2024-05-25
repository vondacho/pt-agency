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

    public boolean includes(LocalDate date) {
        return !(date.isBefore(from) || (to == null || date.isAfter(to)));
    }

    public boolean overlappedBy(Validity other) {
        return includes(other.from)
                || (other.to != null && includes(other.to))
                || (other.from.isBefore(from) && (other.to == null || other.to.isAfter(to)));
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
}

