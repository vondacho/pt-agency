package ch.obya.pta.booking.external.shopping;

import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.domain.ArticleId;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;

@Unremovable
@ApplicationScoped
public class DefaultArticleStore implements ArticleStore {
    @Override
    public Uni<List<ArticleId>> selectMatchingSubscriptions(Set<ArticleId> subscriptions, ArticleId articleId) {
        return null;
    }
}
