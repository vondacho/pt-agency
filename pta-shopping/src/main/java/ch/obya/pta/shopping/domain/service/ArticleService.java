package ch.obya.pta.shopping.domain.service;

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.util.EntityFinder;
import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.common.util.search.FindCriteria;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.event.ArticleRemoved;
import ch.obya.pta.shopping.domain.repository.ArticleRepository;
import ch.obya.pta.shopping.domain.util.ArticleProblem;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;

@ApplicationScoped
public class ArticleService {

    @Inject
    ArticleRepository repository;

    @Inject
    EventPublisher eventPublisher;

    @Transactional
    public Uni<ArticleId> create(Name name,
                                 Text description,
                                 Validity validity,
                                 Set<Tag> profile,
                                 Set<Tag> grants,
                                 Quota quota,
                                 Price price) {

        return checkUniqueness(null, name, validity).replaceWith(
                Uni.createFrom().item(
                                new Article(
                                        name,
                                        description,
                                        validity,
                                        profile,
                                        grants,
                                        quota,
                                        price
                                ))
                        .flatMap(c -> repository.save(c))
                        .flatMap(c -> eventPublisher.publish(c.domainEvents()).replaceWith(c))
                        .map(Article::id));
    }

    @Transactional
    public Uni<Void> modify(ArticleId articleId, Consumer<Article.Modifier> patches) {
        return findOne(articleId)
                .map(c -> {
                    var modifier = c.modify();
                    patches.accept(modifier);
                    modifier.done();
                    return c;
                })
                .flatMap(c -> {
                    var state = c.state();
                    return checkUniqueness(articleId, state.name().standardize(), state.validity()).replaceWith(c);
                })
                .flatMap(c -> repository.save(c))
                .flatMap(c -> eventPublisher.publish(c.domainEvents()));
    }

    private Uni<Void> checkUniqueness(ArticleId exclude, Name name, Validity validity) {
        return repository.findByCriteria(FindCriteria.from("""
                    name:%s,
                    """.formatted(name.standardize())))
                .select().where(c -> !c.id().equals(exclude))
                .onItem()
                .failWith(ArticleProblem.AlreadyExisting.toException(name.standardize(), validity.from(), validity.to()))
                .ifNoItem().after(Duration.ofMillis(100))
                .recoverWithCompletion().toUni().replaceWithVoid();
    }

    @Transactional
    public Uni<Void> remove(ArticleId articleId) {
        return findOne(articleId)
                .flatMap(c -> repository.remove(articleId))
                .flatMap(c -> eventPublisher.publish(Set.of(new ArticleRemoved(articleId))));
    }

    public Uni<Article> findOne(ArticleId id) {
        return EntityFinder.find(Article.class, id, repository::findOne, CommonProblem.EntityNotFound);
    }

    public Multi<ArticleId> eligibleSubscriptionsFor(ArticleId article) {
        var subscriptions = repository.findAllInTags(
                Tag.Default.SUBSCRIPTION.toTag(),
                Tag.Default.PASS.toTag());

        return findOne(article)
                .map(Article::state)
                .toMulti().flatMap(a -> subscriptions.select().where(c -> c.state().grants().containsAll(a.profile())))
                .map(BaseEntity::id);
    }
}
