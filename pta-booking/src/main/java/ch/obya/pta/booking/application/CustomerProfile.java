package ch.obya.pta.booking.application;


import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;
import io.smallrye.mutiny.Multi;

import java.time.LocalDate;

public interface CustomerProfile {

    Multi<Subscription> validSubscriptionsOf(ParticipantId participant, LocalDate validAt);
}
