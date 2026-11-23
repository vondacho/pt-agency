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
        entity.customer = customer;
        entity.logicalId = subscription.id().id();
        entity.update(subscription);
        return entity;
    }

    void update(Subscription subscription) {
        var state = subscription.state();
        this.articleId = subscription.articleId().id();
        this.validFrom = state.validity().from();
        this.validTo = state.validity().to();
        this.quotaMin = state.quota() == null ? null : state.quota().min();
        this.quotaMax = state.quota() == null ? null : state.quota().max();
        this.credits = state.credits();
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
