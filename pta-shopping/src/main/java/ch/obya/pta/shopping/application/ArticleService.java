package ch.obya.pta.shopping.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.util.EntityFinder;
import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.event.ArticleRemoved;
import ch.obya.pta.shopping.domain.repository.ArticleRepository;
import ch.obya.pta.shopping.domain.util.ArticleProblem;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;

import static ch.obya.pta.common.util.search.AttributeFilter.equal;
import static ch.obya.pta.common.util.search.AttributeFilter.notEqual;

@ApplicationScoped
public class ArticleService {

    @Inject
    ArticleRepository repository;

    @Inject
    EventPublisher eventPublisher;

    @WithTransaction
    public Uni<ArticleId> create(Name name,
                                 Text description,
                                 Validity validity,
                                 Set<Tag> profile,
                                 Set<Tag> grants,
                                 Quota quota,
                                 Price price) {
        return checkUniqueness(null, name, validity).replaceWith(
                Uni.createFrom().item(() -> new Article(name.standardize(), description, validity, profile, grants, quota, price))
                        .flatMap(a -> repository.save(a).replaceWith(a))
                        .flatMap(a -> eventPublisher.publish(a.domainEvents()).replaceWith(a))
                        .map(Article::id));
    }

    @WithTransaction
    public Uni<Void> modify(ArticleId articleId, Consumer<Article.Modifier> patches) {
        return findOne(articleId)
                .map(a -> {
                    var modifier = a.modify();
                    patches.accept(modifier);
                    modifier.done();
                    return a;
                })
                .flatMap(a -> checkUniqueness(articleId, a.state().name(), a.state().validity()).replaceWith(a))
                .flatMap(a -> repository.save(a).replaceWith(a))
                .flatMap(a -> eventPublisher.publish(a.domainEvents()));
    }

    /**
     * An article name is unique: another article with the same (standardized) name is a conflict.
     */
    private Uni<Void> checkUniqueness(@Nullable ArticleId exclude, Name name, Validity validity) {
        var standardized = name.standardize();
        var criteria = exclude == null
                ? Map.of("name", List.of(equal(standardized)))
                : Map.of("name", List.of(equal(standardized)), "id", List.of(notEqual(exclude)));
        return repository.findByCriteria(criteria)
                .flatMap(found -> found.isEmpty()
                        ? Uni.createFrom().voidItem()
                        : Uni.createFrom().failure(ArticleProblem.AlreadyExisting
                                .toException(standardized.content(), validity.from(), validity.to()).get()));
    }

    @WithTransaction
    public Uni<Void> remove(ArticleId articleId, Boolean force) {
        return findOne(articleId)
                .flatMap(a -> (force ? repository.remove(articleId) : repository.save(a.close()).replaceWithVoid()).replaceWith(a))
                .flatMap(a -> eventPublisher.publish(force ? Set.of(new ArticleRemoved(articleId)) : a.domainEvents()));
    }

    @WithTransaction
    public Uni<Article> findOne(ArticleId id) {
        return EntityFinder.find(Article.class, id, repository::findOne, CommonProblem.EntityNotFound);
    }

    /**
     * Subscriptions and passes that give access to the given article: their grants cover the article's profile.
     */
    @WithTransaction
    public Uni<List<ArticleId>> eligibleSubscriptionsFor(ArticleId article) {
        return findOne(article)
                .map(a -> a.state().profile() == null ? Set.<Tag>of() : a.state().profile())
                .flatMap(profile -> repository.findAllInTags(Tag.Default.SUBSCRIPTION.toTag(), Tag.Default.PASS.toTag())
                        .map(subscriptions -> subscriptions.stream()
                                .filter(s -> s.state().grants() != null && s.state().grants().containsAll(profile))
                                .map(BaseEntity::id)
                                .toList()));
    }
}
