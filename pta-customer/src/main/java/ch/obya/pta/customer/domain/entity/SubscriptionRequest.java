package ch.obya.pta.customer.domain.entity;

import java.time.Instant;

import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.SubscriptionId;
import ch.obya.pta.customer.domain.vo.SubscriptionRequestId;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

/**
 * A customer's request for a subscription article. The agency confirms it (payment at the desk), which creates
 * the subscription, or the customer cancels it while pending.
 *
 * @param subscription the subscription created by the confirmation, if confirmed
 */
public record SubscriptionRequest(
        SubscriptionRequestId id,
        ArticleId articleId,
        Instant requestedAt,
        Status status,
        SubscriptionId subscription) {

    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }

    public SubscriptionRequest {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("SubscriptionRequest.id"));
        ifNullThrow(articleId, CommonProblem.AttributeNotNull.toException("SubscriptionRequest.articleId"));
        ifNullThrow(requestedAt, CommonProblem.AttributeNotNull.toException("SubscriptionRequest.requestedAt"));
        ifNullThrow(status, CommonProblem.AttributeNotNull.toException("SubscriptionRequest.status"));
    }

    public static SubscriptionRequest pending(ArticleId articleId) {
        return new SubscriptionRequest(SubscriptionRequestId.create(), articleId, Instant.now(), Status.PENDING, null);
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public SubscriptionRequest cancelled() {
        return new SubscriptionRequest(id, articleId, requestedAt, Status.CANCELLED, null);
    }

    public SubscriptionRequest confirmed(SubscriptionId subscription) {
        return new SubscriptionRequest(id, articleId, requestedAt, Status.CONFIRMED, subscription);
    }

}
