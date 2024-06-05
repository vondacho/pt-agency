package ch.obya.pta.shopping.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Identity;

import java.util.UUID;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record ArticleId(UUID id) implements Identity {
    public ArticleId {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("ArticleId.id"));
    }

    public static ArticleId create() {
        return new ArticleId(UUID.randomUUID());
    }
}
