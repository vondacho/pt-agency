package ch.obya.pta.common.domain.entity;

import java.util.List;

import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.vo.Identity;

/**
 * This interface specifies the minimal api of a domain entity, basically composed of an identity and a state.
 *
 * @param <E> Any class that implements {@link Entity}
 * @param <I> Any class that implements {@link Identity}
 * @param <S> Any class
 */
public interface Entity<E extends Entity<E, I, S>, I extends Identity, S> {

    /**
     * @return the identity of the entity
     */
    I id();

    /**
     * @return a snapshot of the current state hosted by the entity
     */
    S state();

    /**
     * @return the list of domain {@link Event} instances emitted and accumulated when invoking the entity methods.
     * A second call returns an empty result.
     */
    List<Event> domainEvents();
}
