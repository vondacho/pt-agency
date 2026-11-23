package ch.obya.pta.www.auth;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * The signed-in participant. Their participant id (pta-customer's customer id) is the {@value #CUSTOMER_ID} claim
 * of their token, set by the identity provider: it never comes from the request itself.
 */
@RequestScoped
public class Participant {

    static final String CUSTOMER_ID = "customer_id";

    @Inject
    SecurityIdentity identity;

    public boolean signedIn() {
        return !identity.isAnonymous();
    }

    /** Participant id, when the account is linked to a customer. */
    public Optional<UUID> id() {
        if (!signedIn() || !(identity.getPrincipal() instanceof JsonWebToken token)) {
            return Optional.empty();
        }
        return Optional.ofNullable(token.<Object>getClaim(CUSTOMER_ID))
                .map(Object::toString)
                .map(String::trim)
                .flatMap(Participant::toUuid);
    }

    /** First name, or the user name when the token has none. */
    public String displayName() {
        if (identity.getPrincipal() instanceof JsonWebToken token) {
            var given = token.<Object>getClaim("given_name");
            if (given != null) return given.toString();
            var preferred = token.<Object>getClaim("preferred_username");
            if (preferred != null) return preferred.toString();
        }
        return identity.getPrincipal().getName();
    }

    private static Optional<UUID> toUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.replace("\"", "")));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
