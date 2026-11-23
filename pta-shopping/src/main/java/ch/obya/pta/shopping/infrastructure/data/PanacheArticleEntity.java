package ch.obya.pta.shopping.infrastructure.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;

@Entity(name = "article")
class PanacheArticleEntity extends PanacheEntity {

    UUID logicalId;
    String name;
    String description;
    LocalDate validFrom;
    LocalDate validTo;
    Double price;
    Integer quotaMin;
    Integer quotaMax;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_profile", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    Set<String> profile = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_grant", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    Set<String> grants = new HashSet<>();

    static PanacheQuery<PanacheArticleEntity> findByLogicalId(ArticleId id) {
        return find("logicalId", id.id());
    }

    static PanacheQuery<PanacheArticleEntity> findInProfile(Set<String> tags) {
        return find("select distinct a from article a join a.profile t where t in ?1", tags);
    }

    /**
     * Translates attribute filters into an HQL where-clause with positional parameters,
     * so that several filters on the same attribute do not collide.
     */
    static PanacheQuery<PanacheArticleEntity> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        var clauses = new ArrayList<String>();
        var parameters = new ArrayList<>();
        criteria.forEach((name, filters) -> filters.forEach(filter -> {
            parameters.add(toParameter(filter.value()));
            clauses.add("%s %s ?%d".formatted(toDataField(name), toOperator(filter.operator()), parameters.size()));
        }));
        return clauses.isEmpty() ? findAll() : find(String.join(" and ", clauses), parameters.toArray());
    }

    private static String toOperator(AttributeFilter.Operator operator) {
        return switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUALS -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUALS -> "<=";
            case LIKE -> "like";
        };
    }

    private static Object toParameter(Object value) {
        return switch (value) {
            case ArticleId id -> id.id();
            case Name name -> name.content();
            case Text text -> text.content();
            case Price price -> price.value();
            default -> value;
        };
    }

    private static String toDataField(String name) {
        return switch (name) {
            case "id" -> "logicalId";
            case "name", "price", "validFrom", "validTo" -> name;
            default -> throw new IllegalArgumentException("Unknown attribute: " + name);
        };
    }

    Article toDomain() {
        var article = new Article(
                new ArticleId(logicalId),
                new Name(name),
                new Text(description),
                new Validity(validFrom, validTo),
                toTags(profile),
                toTags(grants),
                quotaMin == null && quotaMax == null ? null : new Quota(quotaMin, quotaMax),
                new Price(price));
        // Rehydration is not a creation: drop the ArticleCreated event raised by the constructor.
        article.domainEvents();
        return article;
    }

    PanacheArticleEntity set(ArticleId id, Article.State state) {
        this.logicalId = id.id();
        this.name = state.name().content();
        this.description = state.description().content();
        this.validFrom = state.validity().from();
        this.validTo = state.validity().to();
        this.price = state.price().value();
        this.quotaMin = state.quota() == null ? null : state.quota().min();
        this.quotaMax = state.quota() == null ? null : state.quota().max();
        this.profile.clear();
        this.profile.addAll(fromTags(state.profile()));
        this.grants.clear();
        this.grants.addAll(fromTags(state.grants()));
        return this;
    }

    private static Set<Tag> toTags(Set<String> tags) {
        return tags.stream().map(Tag::new).collect(Collectors.toSet());
    }

    private static Set<String> fromTags(Set<Tag> tags) {
        return tags == null ? Set.of() : tags.stream().map(Tag::name).collect(Collectors.toSet());
    }
}
