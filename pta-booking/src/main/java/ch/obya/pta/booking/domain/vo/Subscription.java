package ch.obya.pta.booking.domain.vo;


import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.common.domain.util.CommonProblem;
import lombok.Builder;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

@Builder(builderClassName = "Builder", toBuilder = true)
public record Subscription(SubscriptionId id,
                           ArticleId articleId,
                           Validity validity) {

    public Subscription {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Subscription.id"));
        ifNullThrow(articleId, CommonProblem.AttributeNotNull.toException("Subscription.articleId"));
        ifNullThrow(validity, CommonProblem.AttributeNotNull.toException("Subscription.validity"));
    }
}
