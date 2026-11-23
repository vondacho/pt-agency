package ch.obya.pta.www.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Who is signed in, for the SPA. Public: anonymous visitors get {@code authenticated: false}.
 */
@Path("/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    /**
     * @param linked true when the account is linked to a participant, i.e. may book sessions
     */
    public record Me(boolean authenticated, String name, boolean linked) {
    }

    @Inject
    Participant participant;

    @GET
    public Me me() {
        return participant.signedIn()
                ? new Me(true, participant.displayName(), participant.id().isPresent())
                : new Me(false, null, false);
    }
}
