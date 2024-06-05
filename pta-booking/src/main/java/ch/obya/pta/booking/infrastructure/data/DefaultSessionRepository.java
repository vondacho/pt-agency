package ch.obya.pta.booking.infrastructure.data;


import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.util.search.FindCriteria;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;

@ApplicationScoped
public class DefaultSessionRepository implements SessionRepository {

    @Override
    public Uni<Session> findOne(SessionId id) {
        return null;
    }

    @Override
    public Multi<Session> findByCriteria(Collection<FindCriteria> criteria) {
        return null;
    }

    @Override
    public Uni<Session> save(SessionId id, Session.State state) {
        return null;
    }

    @Override
    public Uni<Void> remove(SessionId id) {
        return null;
    }
}
