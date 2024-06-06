package ch.obya.pta.booking.application;

import java.time.LocalDate;

import io.smallrye.mutiny.Multi;

import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;

public interface CustomerProfile {

    Multi<Subscription> validSubscriptionsOf(ParticipantId participant, LocalDate validAt);
}
