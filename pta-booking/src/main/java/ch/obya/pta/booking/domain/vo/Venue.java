package ch.obya.pta.booking.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

/**
 * A place where sessions take place.
 *
 * @param accessNotes how to get there and in, e.g. the room or the meeting point
 * @param mapUrl      a link to a map of the place
 */
public record Venue(VenueId id, String name, String address, String accessNotes, String mapUrl) {
    public Venue {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Venue.id"));
        ifNullThrow(name, CommonProblem.AttributeNotNull.toException("Venue.name"));
        ifEmptyThrow(name, CommonProblem.AttributeNotEmpty.toException("Venue.name"));
    }
}
