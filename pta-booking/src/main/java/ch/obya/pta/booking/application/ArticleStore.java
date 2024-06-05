package ch.obya.pta.booking.application;


import ch.obya.pta.booking.domain.vo.ArticleId;
import io.smallrye.mutiny.Multi;

public interface ArticleStore {

    Multi<ArticleId> eligibleSubscriptionsFor(ArticleId session);
}
