package ch.obya.pta.booking.infrastructure.data;

import java.util.UUID;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import ch.obya.pta.booking.domain.vo.Venue;
import ch.obya.pta.booking.domain.vo.VenueId;

@Entity(name = "venue")
class PanacheVenueEntity extends PanacheEntity {
    UUID logicalId;
    String name;
    String address;
    String accessNotes;
    String mapUrl;

    Venue toDomain() {
        return new Venue(new VenueId(logicalId), name, address, accessNotes, mapUrl);
    }
}
