package ch.obya.pta.booking.application;

import ch.obya.pta.booking.domain.ArticleId;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ArticleStore {

    Uni<List<ArticleId>> selectMatchingSubscriptions(Set<ArticleId> subscriptions, ArticleId articleId);
}
