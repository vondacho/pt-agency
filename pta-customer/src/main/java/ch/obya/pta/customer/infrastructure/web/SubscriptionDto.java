package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.customer.domain.entity.Subscription;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.SubscriptionId;

public record SubscriptionDto(
    SubscriptionId id,
    ArticleId articleId,
    Validity validity,
    Quota quota,
    Integer credits) {

    public static SubscriptionDto from(Subscription subscription) {
        var state = subscription.state();
        return new SubscriptionDto(
                subscription.id(),
                subscription.articleId(),
                state.validity(),
                state.quota(),
                state.credits()
        );
    }
}
