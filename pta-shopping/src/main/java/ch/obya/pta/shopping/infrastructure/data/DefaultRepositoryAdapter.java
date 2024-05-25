package ch.obya.pta.shopping.infrastructure.data;

import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.util.search.FindCriteria;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.repository.ArticleRepository;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Tag;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;

@ApplicationScoped
public class DefaultRepositoryAdapter implements ArticleRepository {

    @Override
    public Multi<Article> findAllInTags(Tag... tags) {
        return null;
    }

    @Override
    public Uni<Article> findOne(ArticleId id) {
        return null;
    }

    @Override
    public Multi<Article> findByCriteria(Collection<FindCriteria> criteria) {
        return null;
    }

    @Override
    public Multi<Article> findAll() {
        return ArticleRepository.super.findAll();
    }

    @Override
    public Uni<Article> save(ArticleId id, Article.State state) {
        return null;
    }

    @Override
    public Uni<Article> save(Entity<Article, ArticleId, Article.State> entity) {
        return ArticleRepository.super.save(entity);
    }

    @Override
    public Uni<Void> remove(ArticleId id) {
        return null;
    }
}
