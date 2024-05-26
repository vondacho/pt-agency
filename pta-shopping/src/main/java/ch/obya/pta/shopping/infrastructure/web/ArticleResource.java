package ch.obya.pta.shopping.infrastructure.web;

/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Text;
import ch.obya.pta.common.util.search.FindCriteria;
import ch.obya.pta.shopping.domain.repository.ArticleRepository;
import ch.obya.pta.shopping.application.ArticleService;
import ch.obya.pta.shopping.domain.vo.ArticleId;
import ch.obya.pta.shopping.domain.vo.Price;
import ch.obya.pta.shopping.domain.vo.Tag;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;

import java.time.LocalDate;

@Produces("application/problem+json")
@Path("/articles")
public class ArticleResource {

    @Inject
    ArticleService service;
    @Inject
    ArticleRepository repository;

    @GET
    public Multi<ArticleDto> list(@RestQuery String filter) {
        return repository.findByCriteria(FindCriteria.from(filter)).map(ArticleDto::from);
    }

    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @GET
    @Path("{id}")
    public Uni<ArticleDto> get(ArticleId id) {
        return repository.findOne(id).map(ArticleDto::from);
    }

    @GET
    @Path("/{id}/subscriptions")
    public Multi<ArticleId> eligibleSubscriptionsFor(ArticleId id) {
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

    @DELETE
    @Path("{id}")
    public Uni<Void> remove(ArticleId id, @RestQuery Boolean force) {
        return service.remove(id, force != null && force);
    }
}
