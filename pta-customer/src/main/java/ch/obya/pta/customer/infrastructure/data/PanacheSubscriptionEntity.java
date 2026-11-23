package ch.obya.pta.customer.infrastructure.data;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.customer.domain.entity.Subscription;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.SubscriptionId;

@Entity(name = "customer_subscription")
class PanacheSubscriptionEntity extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    PanacheCustomerEntity customer;
    UUID logicalId;
    UUID articleId;
    LocalDate validFrom;
    LocalDate validTo;
    Integer quotaMin;
    Integer quotaMax;
    int credits;

    static PanacheSubscriptionEntity of(PanacheCustomerEntity customer, Subscription subscription) {
        var entity = new PanacheSubscriptionEntity();
        var state = subscription.state();
        entity.customer = customer;
        entity.logicalId = subscription.id().id();
        entity.articleId = subscription.articleId().id();
        entity.validFrom = state.validity().from();
        entity.validTo = state.validity().to();
        entity.quotaMin = state.quota() == null ? null : state.quota().min();
        entity.quotaMax = state.quota() == null ? null : state.quota().max();
        entity.credits = state.credits();
        return entity;
    }

    Subscription toDomain() {
        return new Subscription(
                new SubscriptionId(logicalId),
                new Validity(validFrom, validTo),
                quotaMin == null && quotaMax == null ? null : new Quota(quotaMin, quotaMax),
                credits,
                new ArticleId(articleId));
    }
}
