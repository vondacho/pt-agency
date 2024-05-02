package ch.obya.pta.booking.external.clients;

import ch.obya.pta.booking.application.ClientProfile;
import ch.obya.pta.booking.domain.ParticipantId;
import ch.obya.pta.booking.domain.Subscription;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@Unremovable
@ApplicationScoped
public class DefaultClientProfile implements ClientProfile {
    @Override
    public Uni<List<Subscription>> getSubscriptions(ParticipantId participant, LocalDate validAt) {
        return null;
    }
}
