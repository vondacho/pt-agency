package ch.obya.pta.booking.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;

import java.util.UUID;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record SubscriptionId(UUID id) {
    public SubscriptionId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("SubscriptionId.id"));
    }

    public static SubscriptionId create() {
        return new SubscriptionId(UUID.randomUUID());
    }
}
