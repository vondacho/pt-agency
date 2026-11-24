package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDateTime;
import java.util.UUID;

import ch.obya.pta.booking.domain.vo.Instructor;
import ch.obya.pta.booking.domain.vo.Venue;

/**
 * Everything about one session, for its overview.
 *
 * @param articleId         the course (pta-shopping article) the session belongs to
 * @param cancellationHours the course's cancellation deadline in hours, if any (absent too when pta-shopping is unavailable)
 * @param cancellableUntil  until when a booked seat can be cancelled, if a deadline applies
 */
public record SessionDetailsDto(SessionDto session, UUID articleId, InstructorDto instructor, VenueDto venue, String note,
                                Integer cancellationHours, LocalDateTime cancellableUntil) {

    public record InstructorDto(UUID id, String name, String bio) {
        static InstructorDto from(Instructor instructor) {
            return instructor == null ? null : new InstructorDto(instructor.id().id(), instructor.name(), instructor.bio());
        }
    }

    public record VenueDto(UUID id, String name, String address, String accessNotes, String mapUrl) {
        static VenueDto from(Venue venue) {
            return venue == null ? null : new VenueDto(venue.id().id(), venue.name(), venue.address(), venue.accessNotes(), venue.mapUrl());
        }
    }
}
