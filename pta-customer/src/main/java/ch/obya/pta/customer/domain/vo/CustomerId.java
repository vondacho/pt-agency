package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.util.exception.CommonProblem;

import java.util.UUID;

import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;

public record CustomerId(UUID id) implements Identity {
    public CustomerId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("CustomerId.id"));
    }
    public static CustomerId create() {
        return new CustomerId(UUID.randomUUID());
    }
}
