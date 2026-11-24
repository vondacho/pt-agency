package ch.obya.pta.booking.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

/**
 * An instructor leading sessions.
 *
 * @param bio a short presentation for participants
 */
public record Instructor(InstructorId id, String name, String bio) {
    public Instructor {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Instructor.id"));
        ifNullThrow(name, CommonProblem.AttributeNotNull.toException("Instructor.name"));
        ifEmptyThrow(name, CommonProblem.AttributeNotEmpty.toException("Instructor.name"));
    }
}
