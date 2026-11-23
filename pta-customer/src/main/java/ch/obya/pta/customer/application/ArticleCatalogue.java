package ch.obya.pta.customer.application;

import java.time.Period;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.customer.domain.vo.ArticleId;

/**
 * The terms under which a subscription article is sold (pta-shopping).
 */
public interface ArticleCatalogue {

    /**
     * @param duration how long a subscription to the article lasts
     * @param quota    the article's quota, if any: its maximum is the number of sessions included
     */
    record Terms(Period duration, Quota quota) {
        /** Sessions included, 0 when unlimited. */
        public int credits() {
            return quota == null || quota.max() == null ? 0 : quota.max();
        }
    }

    /** Fails with {@code CustomerProblem.UnknownArticle} or {@code CustomerProblem.ServiceUnavailable}. */
    Uni<Terms> termsOf(ArticleId article);
}
