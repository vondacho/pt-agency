package ch.obya.pta.booking.infrastructure.data;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.*;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.vo.ArticleId;
import ch.obya.pta.booking.domain.vo.Location;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.util.search.AttributeFilter;

@Entity(name = "training_session")
class PanacheSessionEntity extends PanacheEntity {

    static final Sort CHRONOLOGICAL = Sort.by("day").and("startTime");

    UUID logicalId;
    UUID articleId;
    String title;
    LocalDate day;
    LocalTime startTime;
    LocalTime stopTime;
    Integer durationMinutes;
    String location;
    Integer quotaMin;
    Integer quotaMax;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    List<PanacheBookingEntity> bookings = new ArrayList<>();

    static PanacheQuery<PanacheSessionEntity> findByLogicalId(SessionId id) {
        return find("logicalId", id.id());
    }

    /**
     * Translates attribute filters into an HQL where-clause with positional parameters,
     * so that several filters on the same attribute (e.g. a date range) do not collide.
     */
    static PanacheQuery<PanacheSessionEntity> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        var clauses = new ArrayList<String>();
        var parameters = new ArrayList<>();
        criteria.forEach((name, filters) -> filters.forEach(filter -> {
            parameters.add(toParameter(filter.value()));
            clauses.add("%s %s ?%d".formatted(toDataField(name), toOperator(filter.operator()), parameters.size()));
        }));
        return clauses.isEmpty()
                ? findAll(CHRONOLOGICAL)
                : find(String.join(" and ", clauses), CHRONOLOGICAL, parameters.toArray());
    }

    private static String toOperator(AttributeFilter.Operator operator) {
        return switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUALS -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUALS -> "<=";
            case LIKE -> "like";
        };
    }

    private static Object toParameter(Object value) {
        return switch (value) {
            case SessionId id -> id.id();
            case ArticleId id -> id.id();
            case Location location -> location.name();
            default -> value;
        };
    }

    private static String toDataField(String name) {
        return switch (name) {
            case "id" -> "logicalId";
            case "articleId", "title", "day", "location" -> name;
            default -> throw new IllegalArgumentException("Unknown attribute: " + name);
        };
    }

    Session toDomain() {
        var id = new SessionId(logicalId);
        return new Session(
                id,
                new ArticleId(articleId),
                title,
                new Session.TimeSlot(day, startTime, stopTime, Duration.ofMinutes(durationMinutes)),
                new Location(location),
                new Quota(quotaMin, quotaMax),
                bookings.stream().map(b -> b.toDomain(id)).collect(Collectors.toSet()));
    }

    PanacheSessionEntity set(SessionId id, ArticleId articleId, Session.State state) {
        this.logicalId = id.id();
        this.articleId = articleId.id();
        this.title = state.title();
        this.day = state.slot().dow();
        this.startTime = state.slot().start();
        this.stopTime = state.slot().stop();
        this.durationMinutes = (int) state.slot().duration().toMinutes();
        this.location = state.location().name();
        this.quotaMin = state.quota().min();
        this.quotaMax = state.quota().max();
        this.bookings.clear();
        state.bookings().forEach(b -> this.bookings.add(PanacheBookingEntity.of(this, b)));
        return this;
    }
}
