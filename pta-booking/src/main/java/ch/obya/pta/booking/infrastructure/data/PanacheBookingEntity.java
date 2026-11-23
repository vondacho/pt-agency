package ch.obya.pta.booking.infrastructure.data;

import java.util.UUID;

import jakarta.persistence.*;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.BookingId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.booking.domain.vo.SubscriptionId;

@Entity(name = "session_booking")
class PanacheBookingEntity extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    PanacheSessionEntity session;
    UUID participantId;
    UUID subscriptionId;
    @Enumerated(EnumType.STRING)
    Booking.Status status;

    static PanacheBookingEntity of(PanacheSessionEntity session, Booking booking) {
        var entity = new PanacheBookingEntity();
        entity.session = session;
        entity.participantId = booking.id().participant().id();
        entity.subscriptionId = booking.subscription().id();
        entity.status = booking.status();
        return entity;
    }

    void update(Booking booking) {
        this.subscriptionId = booking.subscription().id();
        this.status = booking.status();
    }

    Booking toDomain(SessionId sessionId) {
        return Booking.of(
                new BookingId(sessionId, new ParticipantId(participantId)),
                new SubscriptionId(subscriptionId),
                status);
    }
}
