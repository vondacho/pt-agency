package ch.obya.pta.booking.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;

import java.util.UUID;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record ParticipantId(UUID id) {
    public ParticipantId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("ParticipantId.id"));
    }

    public static ParticipantId create() {
        return new ParticipantId(UUID.randomUUID());
    }
}
