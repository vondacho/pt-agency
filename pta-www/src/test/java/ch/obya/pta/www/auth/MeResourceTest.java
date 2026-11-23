package ch.obya.pta.www.auth;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class MeResourceTest {

    @Test
    void anonymous_visitor_is_not_authenticated() {
        given().when().get("/api/me").then().statusCode(200)
                .body("authenticated", equalTo(false), "linked", equalTo(false), "name", nullValue());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = {
            @Claim(key = "given_name", value = "Alice"),
            @Claim(key = "customer_id", value = "20000000-0000-0000-0000-00000000000a")})
    void linked_participant_is_authenticated_and_linked() {
        given().when().get("/api/me").then().statusCode(200)
                .body("authenticated", equalTo(true), "linked", equalTo(true), "name", equalTo("Alice"));
    }

    @Test
    @TestSecurity(user = "carol")
    void account_without_customer_id_is_not_linked() {
        given().when().get("/api/me").then().statusCode(200)
                .body("authenticated", equalTo(true), "linked", equalTo(false), "name", equalTo("carol"));
    }
}
