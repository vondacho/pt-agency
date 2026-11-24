package ch.obya.pta.booking.domain.vo;

import java.util.UUID;

import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Identity;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record InstructorId(UUID id) implements Identity {
    public InstructorId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("InstructorId.id"));
    }

    public static InstructorId create() {
        return new InstructorId(UUID.randomUUID());
    }
}
