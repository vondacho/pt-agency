package ch.obya.pta.shopping.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.shopping.application.ArticleService;
import ch.obya.pta.shopping.domain.aggregate.Article;
import ch.obya.pta.shopping.domain.repository.ArticleRepository;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;

@Produces({"application/json", "application/problem+json"})
@Path("/articles")
public class ArticleResource {

    @Inject
    ArticleService service;
    @Inject
    ArticleRepository repository;

    /**
     * Lists articles, optionally filtered (filters combine):
     * <ul>
     *     <li>by name: {@code ?name=eq:Unlimited pass} or {@code ?name=like:%pass%}</li>
     *     <li>by kind, i.e. profile tags (any of them): {@code ?tag=SUBSCRIPTION&tag=PASS}</li>
     *     <li>to the articles on sale today: {@code ?onSale=true}</li>
     * </ul>
     */
    @GET
    @WithSession // filters may run several queries: one reactive session for all of them
    public Uni<List<ArticleDto>> list(@RestQuery List<String> name, @RestQuery List<String> tag, @RestQuery boolean onSale) {
        Uni<List<Article>> byName = name.isEmpty() ? null
                : repository.findByCriteria(Map.of("name", AttributeFilter.from(name, Name::new)));
        Uni<List<Article>> byTag = tag.isEmpty() ? null
                : repository.findAllInTags(tag.stream().map(Tag::new).toArray(Tag[]::new));
        Uni<List<Article>> articles;
        if (byName != null && byTag != null) {
            articles = byName.flatMap(named -> byTag.map(tagged -> tagged.stream()
                    .filter(a -> named.stream().anyMatch(n -> n.id().equals(a.id())))
                    .toList()));
        } else if (byName != null) {
            articles = byName;
        } else if (byTag != null) {
            articles = byTag;
        } else {
            articles = repository.findAll();
        }
        var today = LocalDate.now();
        return articles.map(list -> list.stream()
                .filter(a -> !onSale || a.state().validity().includes(today))
                .map(ArticleDto::from)
                .toList());
    }

    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @GET
    @Path("{id}")
    public Uni<ArticleDto> get(ArticleId id) {
        return service.findOne(id).map(ArticleDto::from);
    }

    /**
     * Subscription and pass articles which give access to the given article.
     */
    @GET
    @Path("/{id}/subscriptions")
    public Uni<List<ArticleId>> eligibleSubscriptionsFor(ArticleId id) {
        return service.eligibleSubscriptionsFor(id);
    }

    @ResponseStatus(201)
    @POST
    public Uni<ArticleId> create(ArticleDto input) {
        return service.create(
                input.name(),
                input.description(),
                input.validity(),
                input.profile(),
                input.grants(),
                input.quota(),
                input.price());
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}")
    public Uni<Void> modify(ArticleId id, ArticleDto input) {
        return service.modify(id, article -> article
                .rename(input.name(), input.description())
                .price(input.price())
                .openOn(input.validity().from())
                .closeOn(input.validity().to())
                .reallocate(input.quota())
                .tag(input.profile(), input.grants()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}")
    public Uni<Void> modify(ArticleId id,
                            @RestQuery Name name,
                            @RestQuery Text description,
                            @RestQuery Price price,
                            @RestQuery LocalDate validFrom,
                            @RestQuery LocalDate validTo,
                            @RestQuery Quota quota,
                            @RestQuery String profile,
                            @RestQuery String grants) {
        return service.modify(id, article -> {
            if (name != null || description != null) article.rename(name, description);
            if (price != null) article.price(price);
            if (validFrom != null) article.openOn(validFrom);
            if (validTo != null) article.closeOn(validTo);
            if (quota != null) article.reallocate(quota);
            if (profile != null || grants != null) article.tag(
                    profile != null ? Tag.fromMany(profile) : null,
                    grants != null ? Tag.fromMany(grants) : null);
        });
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}/naming")
    public Uni<Void> rename(ArticleId id, @RestQuery Name name, @RestQuery Text description) {
        return service.modify(id, article -> article.rename(name, description));
    }

    @ResponseStatus(204)
    @DELETE
    @Path("{id}")
    public Uni<Void> remove(ArticleId id, @RestQuery Boolean force) {
        return service.remove(id, force != null && force);
    }
}
