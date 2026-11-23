package ch.obya.pta.shopping.infrastructure.web;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.shopping.application.ArticleService;
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
     * Lists articles, optionally filtered by name (`?name=eq:Unlimited pass` or `?name=like:%pass%`).
     */
    @GET
    public Uni<List<ArticleDto>> list(@RestQuery List<String> name) {
        Map<String, List<AttributeFilter>> criteria = new HashMap<>();
        if (!name.isEmpty()) criteria.put("name", AttributeFilter.from(name, Name::new));
        return repository.findByCriteria(criteria).map(l -> l.stream().map(ArticleDto::from).toList());
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
