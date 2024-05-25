package ch.obya.pta.shopping.domain.aggregate;

import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.shopping.domain.event.ArticleCreated;
import ch.obya.pta.shopping.domain.event.ArticleModified;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

@Accessors(fluent = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class Article extends BaseEntity<Article, ArticleId, Article.State> {

    @Builder(builderClassName = "Builder", toBuilder = true, access = AccessLevel.PRIVATE)
    public record State(
            Name name,
            Text description,
            Validity validity,
            Price price,
            Set<Tag> profile,
            Set<Tag> grants,
            Quota quota) {

        public State {
            ifNullThrow(name, CommonProblem.AttributeNotNull.toException("Article.name"));
            ifNullThrow(description, CommonProblem.AttributeNotNull.toException("Article.description"));
            ifNullThrow(validity, CommonProblem.AttributeNotNull.toException("Article.validity"));
            ifNullThrow(price, CommonProblem.AttributeNotNull.toException("Article.price"));
        }
    }

    public Article(ArticleId id, Name name, Text description, Validity validity, Set<Tag> profile, Set<Tag> grants, Quota quota, Price price) {
        super(id, new State(name, description, validity, price, profile, grants, quota));
        andEvent(new ArticleCreated(id));
    }

    public Article(Name name, Text description, Validity validity, Set<Tag> profile, Set<Tag> grants, Quota quota, Price price) {
        this(ArticleId.create(), name, description, validity, profile, grants, quota, price);
    }

    @Override
    protected Article.State cloneState() {
        return state.toBuilder().build();
    }


    public Article.Modifier modify() {
        return this.new Modifier();
    }

    public class Modifier {
        private final State.Builder stateBuilder = state.toBuilder();

        public Modifier rename(Name name, Text description) {
            if (name != null) stateBuilder.name(name.standardize());
            if (description != null) stateBuilder.description(description);
            return this;
        }

        public Modifier price(Price price) {
            if (price != null) stateBuilder.price(price);
            return this;
        }

        public Modifier openOn(LocalDate date) {
            if (date != null) Validity.openOn(date);
            return this;
        }

        public Modifier closeOn(LocalDate date) {
            if (date != null) stateBuilder.validity.closeOn(date);
            return this;
        }

        public Modifier tag(Set<Tag> profile, Set<Tag> grants) {
            if (profile != null) stateBuilder.profile(profile);
            if (grants != null) stateBuilder.grants(grants);
            return this;
        }

        public Modifier reallocate(Quota quota) {
            if (quota != null) stateBuilder.quota(quota);
            return this;
        }

        public Modifier consume(int count) {
            stateBuilder.quota(state.quota.consume(count));
            return this;
        }

        public void done() {
            state = stateBuilder.build();
            andEvent(new ArticleModified(id));
        }
    }

}

