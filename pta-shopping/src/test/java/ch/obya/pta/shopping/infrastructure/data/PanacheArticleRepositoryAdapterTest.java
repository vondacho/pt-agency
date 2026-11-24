package ch.obya.pta.shopping.infrastructure.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;

import static ch.obya.pta.common.util.search.AttributeFilter.equal;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PanacheArticleRepositoryAdapterTest {

    static final ArticleId UNLIMITED_PASS = new ArticleId(java.util.UUID.fromString("50000000-0000-0000-0000-000000000001"));
    static final ArticleId PT_10X = new ArticleId(java.util.UUID.fromString("50000000-0000-0000-0000-000000000002"));

    @Inject
    PanacheArticleRepositoryAdapter repository;

    private static Article article(String name, Set<Tag> profile, Set<Tag> grants, Quota quota) {
        return new Article(new Name(name), new Text("description of " + name),
                new Validity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
                profile, grants, quota, new Price(42.5));
    }

    @RunOnVertxContext
    @Test
    void saved_article_should_be_found_with_the_same_state_and_without_creation_event(UniAsserter asserter) {
        var article = article("Yoga " + System.nanoTime(),
                Set.of(Tag.Default.SESSION.toTag(), Tag.Default.COURSE.toTag()), Set.of(), Quota.of(1, 12));

        asserter.assertThat(
                () -> repository.save(article).flatMap(repository::findOne),
                found -> {
                    assertThat(found.id()).isEqualTo(article.id());
                    assertThat(found.state()).isEqualTo(article.state());
                    assertThat(found.domainEvents()).isEmpty();
                });
    }

    @RunOnVertxContext
    @Test
    void updated_tags_should_replace_the_previous_ones(UniAsserter asserter) {
        var article = article("Pilates " + System.nanoTime(), Set.of(Tag.Default.SESSION.toTag()), Set.of(), null);

        asserter.execute(() -> repository.save(article));
        asserter.execute(() -> article.modify().tag(Set.of(Tag.Default.COURSE.toTag()), Set.of(Tag.Default.SAUNA.toTag())).done());
        asserter.assertThat(
                () -> repository.save(article.id(), article.state()).flatMap(repository::findOne),
                found -> {
                    assertThat(found.state().profile()).containsExactly(Tag.Default.COURSE.toTag());
                    assertThat(found.state().grants()).containsExactly(Tag.Default.SAUNA.toTag());
                    assertThat(found.state().quota()).isNull();
                });
    }

    @RunOnVertxContext
    @Test
    void articles_should_be_found_by_profile_tags(UniAsserter asserter) {
        asserter.assertThat(
                () -> repository.findAllInTags(Tag.Default.SUBSCRIPTION.toTag(), Tag.Default.PASS.toTag()),
                found -> assertThat(found).extracting(Article::id).contains(UNLIMITED_PASS, PT_10X));
    }

    @RunOnVertxContext
    @Test
    void articles_should_be_found_by_name(UniAsserter asserter) {
        asserter.assertThat(
                () -> repository.findByCriteria(Map.of("name", List.of(equal(new Name("Unlimited pass"))))),
                found -> assertThat(found).extracting(Article::id).containsExactly(UNLIMITED_PASS));
    }

    @RunOnVertxContext
    @Test
    void unknown_article_should_not_be_found(UniAsserter asserter) {
        asserter.assertThat(() -> repository.findOne(ArticleId.create()), found -> assertThat(found).isNull());
    }

    @RunOnVertxContext
    @Test
    void course_details_should_be_kept_with_the_objectives_in_order(UniAsserter asserter) {
        var article = article("Spinning " + System.nanoTime(), Set.of(Tag.Default.SESSION.toTag(), Tag.Default.COURSE.toTag()), Set.of(), null);
        var course = new ch.obya.pta.shopping.domain.vo.CourseDetails(
                List.of("Warm up", "Intervals", "Cool down"), "All levels", "From 14 years", "Water", 12);
        article.modify().describe(course).done();

        asserter.assertThat(
                () -> repository.save(article).flatMap(repository::findOne),
                found -> {
                    assertThat(found.state().course()).isEqualTo(course);
                    assertThat(found.state().course().objectives()).containsExactly("Warm up", "Intervals", "Cool down");
                    assertThat(found.domainEvents()).isEmpty();
                });
    }
}
