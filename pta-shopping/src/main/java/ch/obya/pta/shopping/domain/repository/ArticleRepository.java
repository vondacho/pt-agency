package ch.obya.pta.shopping.domain.repository;

import ch.obya.pta.common.domain.repository.EntityRepository;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Tag;
import io.smallrye.mutiny.Multi;

public interface ArticleRepository extends EntityRepository<Article, ArticleId, Article.State> {

    Multi<Article> findAllInTags(Tag...tags);
}

