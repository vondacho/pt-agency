package ch.obya.pta.booking.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record Location(String name) {
    public Location {
        ifNullThrow(name, CommonProblem.AttributeNotNull.toException("Location.name"));
        ifEmptyThrow(name, CommonProblem.AttributeNotEmpty.toException("Location.name"));
    }
}
