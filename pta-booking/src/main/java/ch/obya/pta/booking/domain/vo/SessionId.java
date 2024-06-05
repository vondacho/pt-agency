package ch.obya.pta.booking.domain.vo;


import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.domain.util.CommonProblem;

import java.util.UUID;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record SessionId(UUID id) implements Identity {
    public SessionId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("SessionId.id"));
    }

    public static SessionId create() {
        return new SessionId(UUID.randomUUID());
    }

    public static SessionId parse(String s) {
        return new SessionId(UUID.fromString(s));
    }
}
