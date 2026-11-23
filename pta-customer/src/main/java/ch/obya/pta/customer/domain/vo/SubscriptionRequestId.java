package ch.obya.pta.customer.domain.vo;

import java.util.UUID;

import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Identity;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record SubscriptionRequestId(UUID id) implements Identity {
    public SubscriptionRequestId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("SubscriptionRequestId.id"));
    }

    public static SubscriptionRequestId create() {
        return new SubscriptionRequestId(UUID.randomUUID());
    }
}
