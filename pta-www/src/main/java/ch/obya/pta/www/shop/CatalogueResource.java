package ch.obya.pta.www.shop;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.Problems;

/**
 * The shop's catalogue: subscriptions and passes on sale. Public. Served under {@code /api/catalogue}.
 */
@Path("/catalogue")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogueResource {

    @Inject
    Catalogue catalogue;

    @GET
    public Uni<List<Catalogue.Item>> items() {
        return catalogue.items()
                .onFailure().transform(f -> Problems.relay(f, status -> null, "The catalogue is temporarily unavailable."));
    }
}
