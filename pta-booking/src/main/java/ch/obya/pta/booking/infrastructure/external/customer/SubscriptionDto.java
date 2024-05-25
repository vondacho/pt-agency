package ch.obya.pta.booking.infrastructure.external.customer;

import ch.obya.pta.booking.domain.vo.ArticleId;
import ch.obya.pta.booking.domain.vo.SubscriptionId;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Validity;

public record SubscriptionDto(
    SubscriptionId id,
    ArticleId articleId,
    Validity validity,
    Quota quota,
    Integer credits) {
}
