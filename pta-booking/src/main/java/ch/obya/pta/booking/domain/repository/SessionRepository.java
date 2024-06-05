package ch.obya.pta.booking.domain.repository;


import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.repository.EntityRepository;

public interface SessionRepository extends EntityRepository<Session, SessionId, Session.State> {
}
