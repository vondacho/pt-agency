package ch.obya.pta.customer.infrastructure.external;

import java.time.Period;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.customer.application.ArticleCatalogue;
import ch.obya.pta.customer.domain.util.CustomerProblem;
import ch.obya.pta.customer.domain.vo.ArticleId;

/**
 * Article terms from pta-shopping. The duration comes from the article's duration tag (ONE_YEAR, ONE_MONTH…).
 */
@ApplicationScoped
public class ArticleCatalogueAdapter implements ArticleCatalogue {

    /** Duration of an article without a duration tag. */
    static final Period DEFAULT_DURATION = Period.ofMonths(12);

    @RestClient
    ShoppingClient client;

    @Override
    public Uni<Terms> termsOf(ArticleId article) {
        return client.article(article.id())
                .map(a -> new Terms(durationOf(a.profile()), a.quota() == null ? null : new Quota(a.quota().min(), a.quota().max())))
                .onFailure().transform(f -> f instanceof WebApplicationException wae && wae.getResponse().getStatus() == 404
                        ? CustomerProblem.UnknownArticle.toException(article.id()).get()
                        : CustomerProblem.ServiceUnavailable.toException("pta-shopping", f.getMessage()).get());
    }

    static Period durationOf(List<ArticleView.TagView> profile) {
        if (profile == null) return DEFAULT_DURATION;
        return profile.stream()
                .map(ArticleView.TagView::name)
                .map(ArticleCatalogueAdapter::durationOf)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(DEFAULT_DURATION);
    }

    private static Period durationOf(String tag) {
        return switch (tag) {
            case "ONE_YEAR" -> Period.ofYears(1);
            case "THREE_MONTHS" -> Period.ofMonths(3);
            case "TWO_MONTHS" -> Period.ofMonths(2);
            case "ONE_MONTH" -> Period.ofMonths(1);
            case "ONE_WEEK" -> Period.ofWeeks(1);
            case "ONE_DAY", "ONE_SESSION" -> Period.ofDays(1);
            default -> null;
        };
    }

    /** The part of pta-shopping's article representation needed here. */
    public record ArticleView(List<TagView> profile, QuotaView quota) {
        public record TagView(String name) {
        }

        public record QuotaView(Integer min, Integer max) {
        }
    }

    @Path("/articles")
    @RegisterRestClient(configKey = "shopping")
    public interface ShoppingClient {
        @GET
        @Path("/{id}")
        Uni<ArticleView> article(@RestPath UUID id);
    }
}
