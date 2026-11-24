package ch.obya.pta.shopping.infrastructure.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

/**
 * Runs against the seeded articles (db/dev/R__seed_articles.sql).
 */
@TestHTTPEndpoint(ArticleResource.class)
@QuarkusTest
class ArticleResourceTest {

    static final String PERSONAL_TRAINING = "10000000-0000-0000-0000-000000000001";
    static final String SMALL_GROUP_HIIT = "10000000-0000-0000-0000-000000000002";
    static final String UNLIMITED_PASS = "50000000-0000-0000-0000-000000000001";
    static final String PT_10X = "50000000-0000-0000-0000-000000000002";

    @Test
    void personal_training_should_be_accessible_with_both_passes() {
        given().when().get("/{id}/subscriptions", PERSONAL_TRAINING)
                .then().statusCode(200)
                .body("id", containsInAnyOrder(UNLIMITED_PASS, PT_10X));
    }

    @Test
    void group_course_should_be_accessible_with_group_passes_but_not_personal_training_ones() {
        // eligibility ignores whether an article is still on sale: a summer pass bought earlier still opens courses
        given().when().get("/{id}/subscriptions", SMALL_GROUP_HIIT)
                .then().statusCode(200)
                .body("id", containsInAnyOrder(UNLIMITED_PASS,
                        "50000000-0000-0000-0000-000000000003", "50000000-0000-0000-0000-000000000004",
                        "50000000-0000-0000-0000-000000000005", "50000000-0000-0000-0000-000000000006"));
    }

    @Test
    void eligibility_of_an_unknown_article_should_fail_with_404() {
        given().when().get("/{id}/subscriptions", UUID.randomUUID())
                .then().statusCode(404)
                .contentType("application/problem+json")
                .body("code", equalTo("EntityNotFound"));
    }

    @Test
    void article_should_be_returned_with_all_its_attributes() {
        given().when().get("/{id}", UNLIMITED_PASS)
                .then().statusCode(200)
                .body("name.content", equalTo("Unlimited pass"),
                        "price.value", equalTo(1200.0f),
                        "profile.name", hasItems("SUBSCRIPTION", "ONE_YEAR"),
                        "grants.name", containsInAnyOrder("SESSION", "COURSE", "PERSONAL_TRAINING"));
    }

    @Test
    void unknown_article_should_fail_with_404() {
        given().when().get("/{id}", UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    void creating_an_article_with_an_existing_name_should_fail_with_409() {
        var body = """
                {"name":{"content":"Unlimited pass"},"description":{"content":"duplicate"},
                 "validity":{"from":"2026-01-01"},"price":{"value":10.0},"profile":[{"name":"PASS"}],"grants":[]}""";
        given().contentType("application/json").body(body)
                .when().post()
                .then().statusCode(409)
                .body("code", equalTo("AlreadyExisting"));
    }

    @Test
    void created_article_should_be_listed_by_name() {
        var name = "Aqua gym " + System.nanoTime();
        var body = """
                {"name":{"content":"%s"},"description":{"content":"In the pool"},
                 "validity":{"from":"2026-01-01"},"price":{"value":18.0},"profile":[{"name":"SESSION"},{"name":"COURSE"}],"grants":[]}"""
                .formatted(name);
        var id = given().contentType("application/json").body(body)
                .when().post()
                .then().statusCode(201)
                .extract().path("id");

        given().queryParam("name", "eq:" + name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase())
                .when().get()
                .then().statusCode(200)
                .body("id.id", containsInAnyOrder(id));
    }

    @Test
    void catalogue_lists_subscriptions_and_passes_on_sale() {
        given().queryParam("tag", "SUBSCRIPTION").queryParam("tag", "PASS").queryParam("onSale", true)
                .when().get()
                .then().statusCode(200)
                .body("id.id", containsInAnyOrder(UNLIMITED_PASS, PT_10X,
                        "50000000-0000-0000-0000-000000000003", "50000000-0000-0000-0000-000000000004",
                        "50000000-0000-0000-0000-000000000005"));
    }

    @Test
    void articles_no_longer_on_sale_are_listed_unless_excluded() {
        given().queryParam("tag", "SUBSCRIPTION")
                .when().get()
                .then().statusCode(200)
                .body("id.id", hasItems("50000000-0000-0000-0000-000000000006"));
    }

    @Test
    void filters_combine() {
        given().queryParam("tag", "PASS").queryParam("name", "like:%group%")
                .when().get()
                .then().statusCode(200)
                .body("id.id", containsInAnyOrder("50000000-0000-0000-0000-000000000004"));
    }

    @Test
    void a_course_tells_its_objectives_and_conditions() {
        given().when().get("/{id}", "10000000-0000-0000-0000-000000000003")
                .then().statusCode(200)
                .body("name.content", equalTo("Bootcamp"),
                        "course.level", equalTo("All levels"),
                        "course.cancellationHours", equalTo(12),
                        "course.objectives", contains("Outdoor circuits combining cardio and strength", "Team spirit and motivation in a group"),
                        "course.whatToBring", equalTo("Clothes for the weather, running shoes, water and a mat."));
    }
}
