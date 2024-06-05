package ch.obya.pta.common.application;

import java.util.Collection;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.event.Event;

public interface EventPublisher {

    Uni<Void> publish(Collection<Event> events);
}
