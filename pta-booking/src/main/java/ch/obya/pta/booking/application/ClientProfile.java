package ch.obya.pta.booking.application;

import ch.obya.pta.booking.domain.ParticipantId;
import ch.obya.pta.booking.domain.Subscription;
import io.smallrye.mutiny.Uni;

import java.time.LocalDate;
import java.util.List;

public interface ClientProfile {

    Uni<List<Subscription>> getSubscriptions(ParticipantId participant, LocalDate validAt);
}
