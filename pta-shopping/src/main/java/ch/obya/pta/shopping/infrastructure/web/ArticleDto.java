package ch.obya.pta.shopping.infrastructure.web;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;
import lombok.Builder;

import java.util.Set;

@Builder(builderClassName = "Builder", toBuilder = true)
public record ArticleDto(
        ArticleId id,
        Name name,
        Text description,
        Validity validity,
        Price price,
        Set<Tag> profile,
        Set<Tag> grants,
        Quota quota
) {
    public static ArticleDto from(Article article) {
        return ArticleDto.builder().id(article.id()).build();
    }
}
