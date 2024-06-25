package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.customer.domain.vo.CustomerId;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static ch.obya.pta.customer.domain.util.Samples.johnDoe;
import static ch.obya.pta.customer.domain.util.Samples.sherlockHolmes;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestHTTPEndpoint(CustomerResource.class)
@QuarkusTest
class CustomerResourceIT {

    final CustomerDto johnDoe = CustomerDto.from(johnDoe()).toBuilder().id(null).build();
    final CustomerDto sherlockHolmes = CustomerDto.from(sherlockHolmes()).toBuilder().id(null).build();

    static CustomerId id;

    @Order(1)
    @RunOnVertxContext
    @Test
    void should_create_customer() {
        assertThat(id = given()
                .contentType(ContentType.JSON)
                .body(johnDoe)
                .when()
                .post()
                .then()
                .log().all()
                .statusCode(201)
                .extract().jsonPath().getObject("", CustomerId.class)).isNotNull();
        assertThat(id.id()).isNotNull();
    }

    @Order(2)
    @RunOnVertxContext
    @Test
    void should_get_customer() {
        assertThat(id).isNotNull();
        assertThat(
                given()
                        .when()
                        .get("/{id}", Map.of("id", id.id()))
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract().jsonPath().getObject("", CustomerDto.class))
                .extracting(CustomerDto::firstName, CustomerDto::lastName)
                .containsExactly(johnDoe.firstName(), johnDoe.lastName());
    }

    @Order(3)
    @RunOnVertxContext
    @Test
    void should_list_customers() {
        assertThat(
                given()
                        .when()
                        .get()
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract().jsonPath().getList("", CustomerDto.class))
                .extracting(CustomerDto::firstName, CustomerDto::lastName)
                .contains(tuple(johnDoe.firstName(), johnDoe.lastName()));
    }

    @Order(4)
    @RunOnVertxContext
    @Test
    void should_search_customers() {
        assertThat(
                given()
                    .when()
                    .queryParams("firstName", "ne:jude")
                    .queryParams("lastName", "eq:Doe")
                    .queryParams("birth", "eq:1966-04-05")
                    .queryParams("gender", "eq:MALE")
                    .get()
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("", CustomerDto.class))
                .extracting(CustomerDto::firstName, CustomerDto::lastName)
                .contains(tuple(johnDoe.firstName(), johnDoe.lastName()));
    }

    @Order(5)
    @RunOnVertxContext
    @Test
    void should_replace_customer() {
        given()
                .contentType(ContentType.JSON)
                .body(sherlockHolmes)
                .when()
                .put("/{id}", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        //get
        assertThat(
                given()
                    .when()
                    .get("/{id}", Map.of("id", id.id()))
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getObject("", CustomerDto.class))
                .extracting(
                        CustomerDto::firstName,
                        CustomerDto::lastName,
                        CustomerDto::birth,
                        CustomerDto::delivery)
                .containsExactly(
                        sherlockHolmes.firstName(),
                        sherlockHolmes.lastName(),
                        sherlockHolmes.birth(),
                        sherlockHolmes.delivery());
    }

    @Order(6)
    @RunOnVertxContext
    @Test
    void should_update_customer() {
        given()
                .when()
                .queryParams("email", johnDoe.email().address())
                .queryParams("phone", johnDoe.phone().number())
                .patch("/{id}", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        //update
        given()
                .when()
                .queryParams("firstName", johnDoe.firstName().content(), "lastName", johnDoe.lastName().content())
                .patch("/{id}/naming", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        //update
        given()
                .when()
                .queryParams("birth", johnDoe.birth().toISO())
                .queryParams("gender", johnDoe.gender())
                .patch("/{id}/definition", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        //update
        given()
                .when()
                .queryParams("email", johnDoe.email().address())
                .queryParams("phone", johnDoe.phone().number())
                .patch("/{id}/connection", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        //update
        var delivery = "%s,%s,%s,%s,%s".formatted(johnDoe.delivery().streetNo(), johnDoe.delivery().zip(), johnDoe.delivery().city(), johnDoe.delivery().region(), johnDoe.delivery().country());
        given()
                .when()
                .queryParams("delivery", delivery)
                .patch("/{id}/location", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        //get
        assertThat(
                given()
                        .when()
                        .get("/{id}", Map.of("id", id.id()))
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract().jsonPath().getObject("", CustomerDto.class))
                .extracting(
                        CustomerDto::firstName,
                        CustomerDto::lastName,
                        CustomerDto::birth,
                        CustomerDto::delivery,
                        CustomerDto::email,
                        CustomerDto::phone)
                .containsExactly(
                        johnDoe.firstName(),
                        johnDoe.lastName(),
                        johnDoe.birth(),
                        johnDoe.delivery(),
                        johnDoe.email(),
                        johnDoe.phone());
    }

    @Order(7)
    @RunOnVertxContext
    @Test
    void should_remove_customer() {
        //remove
        given()
                .when()
                .queryParams("force", "true")
                .delete("/{id}", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(204);

        given()
                .when()
                .get("/{id}", Map.of("id", id.id()))
                .then()
                .log().all()
                .statusCode(404);
    }
}