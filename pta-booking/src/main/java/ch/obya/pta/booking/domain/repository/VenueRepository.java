package ch.obya.pta.booking.domain.repository;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.vo.Venue;
import ch.obya.pta.booking.domain.vo.VenueId;

/** Places where sessions take place (read-only for now: managed through the seed until a staff UI exists). */
public interface VenueRepository {

    /** The venue, or a null item when unknown. */
    Uni<Venue> findOne(VenueId id);
}
