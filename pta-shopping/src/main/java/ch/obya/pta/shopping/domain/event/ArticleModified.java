package ch.obya.pta.shopping.domain.event;

import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.shopping.domain.vo.ArticleId;

import java.time.Instant;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record ArticleModified(ArticleId id, Instant timestamp) implements Event {

    public ArticleModified {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("%s.id".formatted(getClass().getSimpleName())));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public ArticleModified(ArticleId id) {
        this(id, null);
    }
}
