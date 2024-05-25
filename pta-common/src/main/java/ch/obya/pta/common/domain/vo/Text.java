package ch.obya.pta.common.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record Text(String content) {
    public Text {
        ifNullThrow(content, CommonProblem.AttributeNotNull.toException("Text.content"));
        ifEmptyThrow(content, CommonProblem.AttributeNotEmpty.toException("Text.content"));
    }
}
