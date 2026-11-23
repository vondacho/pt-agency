package ch.obya.pta.booking.infrastructure.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.vo.ArticleId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.util.search.AttributeFilter;

@WithTransaction
@ApplicationScoped
public class PanacheSessionRepositoryAdapter implements SessionRepository {

    @Override
    public Uni<Session> findOne(SessionId id) {
        return PanacheSessionEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNotNull().transform(PanacheSessionEntity::toDomain);
    }

    @Override
    public Uni<List<Session>> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        return PanacheSessionEntity.findByCriteria(criteria)
                .list()
                .map(l -> l.stream().map(PanacheSessionEntity::toDomain).toList());
    }

    @Override
    public Uni<List<Session>> findAll() {
        return PanacheSessionEntity.<PanacheSessionEntity>findAll(PanacheSessionEntity.CHRONOLOGICAL)
                .list()
                .map(l -> l.stream().map(PanacheSessionEntity::toDomain).toList());
    }

    @Override
    public Uni<List<Session>> findBookedBy(ParticipantId participant, LocalDate from) {
        return PanacheSessionEntity.findBookedBy(participant.id(), from)
                .list()
                .map(l -> l.stream().map(PanacheSessionEntity::toDomain).toList());
    }

    /**
     * Creates or updates a session. The article reference is not part of {@link Session.State},
     * hence the whole aggregate is required to create a new session.
     */
    @Override
    public Uni<SessionId> save(Entity<Session, SessionId, Session.State> entity) {
        var session = (Session) entity;
        return PanacheSessionEntity.findByLogicalId(session.id())
                .firstResult()
                .replaceIfNullWith(PanacheSessionEntity::new)
                .map(e -> e.set(session.id(), session.articleId(), session.state()))
                .flatMap(e -> e.persist())
                .replaceWith(session.id());
    }

    /**
     * Updates the state of an existing session.
     */
    @Override
    public Uni<SessionId> save(SessionId id, Session.State state) {
        return PanacheSessionEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNull().failWith(CommonProblem.EntityNotFound.toException("Session", id.id()))
                .map(e -> e.set(id, new ArticleId(e.articleId), state))
                .flatMap(e -> e.persist())
                .replaceWith(id);
    }

    @Override
    public Uni<Void> remove(SessionId id) {
        return PanacheSessionEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNotNull().transformToUni(e -> e.delete())
                .replaceWithVoid();
    }
}
