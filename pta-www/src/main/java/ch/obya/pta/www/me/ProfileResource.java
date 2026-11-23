package ch.obya.pta.www.me;

import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ResponseStatus;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.Problems;
import ch.obya.pta.www.auth.Participant;
import ch.obya.pta.www.me.CustomerClient.Address;

/**
 * The signed-in participant's profile, held by pta-customer. Served under {@code /api/me/profile}.
 */
@Path("/me/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    public record Profile(String firstName, String lastName, String email, String phone, Address delivery, Address billing) {
    }

    public record Contact(String email, String phone) {
    }

    /** @param billing absent when the same as delivery */
    public record Addresses(Address delivery, Address billing) {
    }

    @RestClient
    CustomerClient customers;

    @Inject
    Participant participant;

    @GET
    public Uni<Profile> profile() {
        var id = participant.requireLinked();
        return customers.customer(id)
                .map(c -> new Profile(
                        c.firstName() == null ? null : c.firstName().content(),
                        c.lastName() == null ? null : c.lastName().content(),
                        c.email() == null ? null : c.email().address(),
                        c.phone() == null ? null : c.phone().number(),
                        c.delivery(), c.billing()))
                .onFailure().transform(f -> Problems.relay(f, ProfileResource::messageFor, "Your profile is temporarily unavailable."));
    }

    @PUT
    @Path("/contact")
    @ResponseStatus(204)
    public Uni<Void> contact(Contact contact, @HeaderParam(Participant.REQUEST_HEADER) String requestHeader) {
        var id = participant.requireLinked(requestHeader);
        if (contact == null || blank(contact.email()) || blank(contact.phone())) {
            throw Problems.problem(400, "Please give an e-mail address and a phone number.");
        }
        return customers.reconnect(id, contact.email().trim(), contact.phone().trim())
                .onFailure().transform(f -> Problems.relay(f, status -> status == 400
                        ? "Please check the e-mail address and the phone number."
                        : messageFor(status), "Your profile is temporarily unavailable."));
    }

    @PUT
    @Path("/address")
    @ResponseStatus(204)
    public Uni<Void> address(Addresses addresses, @HeaderParam(Participant.REQUEST_HEADER) String requestHeader) {
        var id = participant.requireLinked(requestHeader);
        if (addresses == null || !complete(addresses.delivery())) {
            throw Problems.problem(400, "Please give a postcode, a town and a country.");
        }
        if (addresses.billing() != null && !complete(addresses.billing())) {
            throw Problems.problem(400, "Please complete the billing address, or leave it empty.");
        }
        // pta-customer reads an address as one comma-separated parameter
        if (Stream.of(addresses.delivery(), addresses.billing()).anyMatch(ProfileResource::hasComma)) {
            throw Problems.problem(400, "Addresses cannot contain commas.");
        }
        return customers.relocate(id, addresses.delivery().asParameter(),
                        addresses.billing() == null ? null : addresses.billing().asParameter())
                .onFailure().transform(f -> Problems.relay(f, status -> status == 400
                        ? "Please check the address."
                        : messageFor(status), "Your profile is temporarily unavailable."));
    }

    private static String messageFor(int status) {
        return status == 404 ? "Your participant profile was not found. Please contact the agency." : null;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean complete(Address address) {
        return address != null && !blank(address.zip()) && !blank(address.city()) && !blank(address.country());
    }

    private static boolean hasComma(Address address) {
        return address != null && Stream.of(address.streetNo(), address.zip(), address.city(), address.region(), address.country())
                .anyMatch(v -> v != null && v.contains(","));
    }
}
