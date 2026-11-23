package ch.obya.pta.common.domain.util;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityFinderTest {

    record Thing(String id) {
    }

    @Test
    void found_entity_is_returned() {
        var thing = new Thing("42");
        var found = EntityFinder.find(Thing.class, "42", id -> Uni.createFrom().item(thing), CommonProblem.EntityNotFound)
                .await().atMost(Duration.ofSeconds(1));
        assertThat(found).isSameAs(thing);
    }

    @Test
    void unknown_entity_fails_with_the_given_problem() {
        assertThatThrownBy(() -> EntityFinder.find(Thing.class, "42", id -> Uni.createFrom().nullItem(), CommonProblem.EntityNotFound)
                .await().atMost(Duration.ofSeconds(1)))
                .isInstanceOf(Problem.Exception.class)
                .hasMessage("Thing 42 does not exist.")
                .satisfies(e -> assertThat(((Problem.Exception) e).code()).isEqualTo("EntityNotFound"));
    }

    @Test
    void slow_finder_is_not_mistaken_for_an_unknown_entity() {
        var thing = new Thing("42");
        var found = EntityFinder.find(Thing.class, "42",
                        id -> Uni.createFrom().item(thing).onItem().delayIt().by(Duration.ofMillis(300)),
                        CommonProblem.EntityNotFound)
                .await().atMost(Duration.ofSeconds(2));
        assertThat(found).isSameAs(thing);
    }

    @Test
    void finder_failures_are_propagated() {
        var outage = new IllegalStateException("database unavailable");
        assertThatThrownBy(() -> EntityFinder.find(Thing.class, "42", id -> Uni.createFrom().failure(outage), CommonProblem.EntityNotFound)
                .await().atMost(Duration.ofSeconds(1)))
                .isSameAs(outage);
    }
}
