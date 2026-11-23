package ch.obya.pta.shopping.infrastructure.data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.repository.ArticleRepository;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Tag;

@WithTransaction
@ApplicationScoped
public class PanacheArticleRepositoryAdapter implements ArticleRepository {

    @Override
    public Uni<Article> findOne(ArticleId id) {
        return PanacheArticleEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNotNull().transform(PanacheArticleEntity::toDomain);
    }

    @Override
    public Uni<List<Article>> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        return PanacheArticleEntity.findByCriteria(criteria).list().map(PanacheArticleRepositoryAdapter::toDomain);
    }

    @Override
    public Uni<List<Article>> findAll() {
        return PanacheArticleEntity.<PanacheArticleEntity>findAll().list().map(PanacheArticleRepositoryAdapter::toDomain);
    }

    @Override
    public Uni<List<Article>> findAllInTags(Tag... tags) {
        var names = Arrays.stream(tags).map(Tag::name).collect(Collectors.toSet());
        return PanacheArticleEntity.findInProfile(names).list().map(PanacheArticleRepositoryAdapter::toDomain);
    }

    @Override
    public Uni<ArticleId> save(ArticleId id, Article.State state) {
        return PanacheArticleEntity.findByLogicalId(id)
                .firstResult()
                .replaceIfNullWith(PanacheArticleEntity::new)
                .map(e -> e.set(id, state))
                .flatMap(e -> e.persist())
                .replaceWith(id);
    }

    @Override
    public Uni<Void> remove(ArticleId id) {
        return PanacheArticleEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNotNull().transformToUni(e -> e.delete())
                .replaceWithVoid();
    }

    private static List<Article> toDomain(List<PanacheArticleEntity> entities) {
        return entities.stream().map(PanacheArticleEntity::toDomain).toList();
    }
}
