package ch.obya.pta.booking.application;

import io.smallrye.mutiny.Multi;

import ch.obya.pta.booking.domain.vo.ArticleId;

public interface ArticleStore {

    Multi<ArticleId> eligibleSubscriptionsFor(ArticleId session);
}
