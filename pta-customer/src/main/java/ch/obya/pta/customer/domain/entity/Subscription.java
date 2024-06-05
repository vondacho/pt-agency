package ch.obya.pta.customer.domain.entity;


import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.customer.domain.util.CustomerProblem;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.SubscriptionId;
import io.quarkus.logging.Log;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifThrow;

@Accessors(fluent = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class Subscription extends BaseEntity<Subscription, SubscriptionId, Subscription.State> {

    private ArticleId articleId;

    @Builder(builderClassName = "Builder", toBuilder = true, access = AccessLevel.PRIVATE)
    public record State(Validity validity, Quota quota, int credits) {
        public State {
            ifNullThrow(validity, CommonProblem.AttributeNotNull.toException("Subscription.validity"));
        }
    }

    public Subscription(SubscriptionId id, Validity validity, Quota quota, int credits, ArticleId articleId) {
        super(id, new State(validity, quota, credits));
        this.articleId = articleId;
    }

    public Subscription(Validity validity, Quota quota, int credits, ArticleId articleId) {
        this(SubscriptionId.create(), validity, quota, credits, articleId);
    }

    @Override
    protected Subscription.State cloneState() {
        return state.toBuilder().build();
    }

    public void charge() {
        ifThrow(() -> state.credits == 0, CustomerProblem.SubscriptionWithoutCredit.toException(id()));
        state = state.toBuilder().credits(state.credits - 1).build();
    }

    public void credit() {
        if (state.quota.max() != null && state.credits == state.quota.max()) {
            Log.info("Cannot add more credits to the subscription %s.");
        } else {
            state = state.toBuilder().credits(state.credits + 1).build();
        }
    }

    public boolean overlappedBy(Subscription other) {
        return articleId.equals(other.articleId) && state.validity.overlappedBy(other.state.validity);
    }

    public Subscription.Modifier modify() {
        return this.new Modifier();
    }

    //TODO
    public class Modifier {
        private final Subscription.State.Builder builder = state.toBuilder();

        public void done() {
            state = builder.build();
        }
    }
}
