package ch.obya.pta.common.application;

import java.util.Collection;
import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.command.Command;

public interface CommandPublisher extends Consumer<Command> {
    Uni<Void> send(Collection<Command> commands);
}
