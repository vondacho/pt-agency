package ch.obya.pta.booking.domain.vo;

import java.util.UUID;

import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Identity;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record VenueId(UUID id) implements Identity {
    public VenueId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("VenueId.id"));
    }

    public static VenueId create() {
        return new VenueId(UUID.randomUUID());
    }
}
