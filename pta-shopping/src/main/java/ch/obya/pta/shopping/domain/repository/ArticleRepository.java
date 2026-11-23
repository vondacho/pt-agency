package ch.obya.pta.shopping.domain.repository;

import java.util.List;

import ch.obya.pta.common.domain.repository.EntityRepository;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Tag;
import io.smallrye.mutiny.Uni;

public interface ArticleRepository extends EntityRepository<Article, ArticleId, Article.State> {

    /**
     * Articles whose profile contains at least one of the given tags.
     */
    Uni<List<Article>> findAllInTags(Tag... tags);
}
