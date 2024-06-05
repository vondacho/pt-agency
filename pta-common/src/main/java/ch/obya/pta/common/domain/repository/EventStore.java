package ch.obya.pta.common.domain.repository;

import java.util.function.Consumer;

import ch.obya.pta.common.domain.event.Event;

public interface EventStore extends Consumer<Event> {
}
