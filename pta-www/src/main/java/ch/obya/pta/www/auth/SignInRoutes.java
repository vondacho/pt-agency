package ch.obya.pta.www.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.ext.web.Router;

/**
 * `/login` is an authenticated path (see quarkus.http.auth.permission.sign-in): reaching it runs the OIDC code flow,
 * and once signed in the participant is sent back to the SPA. `/logout` is handled by Quarkus OIDC itself.
 * Registered on the router directly because REST resources live under /api.
 */
@ApplicationScoped
public class SignInRoutes {

    void register(@Observes Router router) {
        router.get("/login").order(-100).handler(rc -> rc.redirect("/"));
    }
}
