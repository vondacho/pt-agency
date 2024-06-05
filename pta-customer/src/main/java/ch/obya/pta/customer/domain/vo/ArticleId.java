package ch.obya.pta.customer.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;

import java.util.UUID;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record ArticleId(UUID id) {
    public ArticleId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("ArticleId.id"));
    }

    public static ArticleId create() {
        return new ArticleId(UUID.randomUUID());
    }
}
