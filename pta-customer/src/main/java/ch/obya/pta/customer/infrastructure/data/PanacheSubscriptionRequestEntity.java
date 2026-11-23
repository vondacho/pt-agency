package ch.obya.pta.customer.infrastructure.data;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import ch.obya.pta.customer.domain.entity.SubscriptionRequest;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.SubscriptionId;
import ch.obya.pta.customer.domain.vo.SubscriptionRequestId;

@Entity(name = "customer_subscription_request")
class PanacheSubscriptionRequestEntity extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    PanacheCustomerEntity customer;
    UUID logicalId;
    UUID articleId;
    Instant requestedAt;
    @Enumerated(EnumType.STRING)
    SubscriptionRequest.Status status;
    UUID subscriptionId;

    static PanacheSubscriptionRequestEntity of(PanacheCustomerEntity customer, SubscriptionRequest request) {
        var entity = new PanacheSubscriptionRequestEntity();
        entity.customer = customer;
        entity.logicalId = request.id().id();
        entity.articleId = request.articleId().id();
        entity.requestedAt = request.requestedAt();
        entity.update(request);
        return entity;
    }

    void update(SubscriptionRequest request) {
        this.status = request.status();
        this.subscriptionId = request.subscription() == null ? null : request.subscription().id();
    }

    SubscriptionRequest toDomain() {
        return new SubscriptionRequest(
                new SubscriptionRequestId(logicalId),
                new ArticleId(articleId),
                requestedAt,
                status,
                subscriptionId == null ? null : new SubscriptionId(subscriptionId));
    }
}
