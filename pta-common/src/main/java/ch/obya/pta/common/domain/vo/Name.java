package ch.obya.pta.common.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record Name(String content) {
    public Name {
        ifNullThrow(content, CommonProblem.AttributeNotNull.toException("Name.content"));
        ifEmptyThrow(content, CommonProblem.AttributeNotEmpty.toException("Name.content"));
    }

    public Name standardize() {
        var contentLowerCase = content.toLowerCase();
        return new Name(contentLowerCase.substring(0, 1).toUpperCase() + contentLowerCase.substring(1));
    }
}
