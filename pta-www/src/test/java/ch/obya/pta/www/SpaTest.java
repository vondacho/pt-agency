package ch.obya.pta.www;

import org.junit.jupiter.api.Test;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Checks that the built Angular SPA is served, including for client-side routes, without shadowing the API.
 * Quinoa is disabled in tests by default: this profile enables it and also runs the Angular unit tests (npm test).
 */
@QuarkusTest
@TestProfile(QuinoaTestProfiles.EnableAndRunTests.class)
class SpaTest {

    @Test
    void spa_should_be_served_at_root() {
        given().when().get("/").then()
                .statusCode(200)
                .contentType("text/html")
                .body(containsString("<app-root>"), containsString("Upcoming sessions"));
    }

    @Test
    void client_side_routes_should_fall_back_to_the_spa() {
        given().when().get("/some/client/route").then()
                .statusCode(200)
                .body(containsString("<app-root>"));
    }

    @Test
    void api_paths_should_not_be_shadowed_by_the_spa() {
        given().when().get("/api/unknown").then().statusCode(404);
    }
}
