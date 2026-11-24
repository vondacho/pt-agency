package ch.obya.pta.booking.infrastructure.data;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.repository.InstructorRepository;
import ch.obya.pta.booking.domain.repository.VenueRepository;
import ch.obya.pta.booking.domain.vo.Instructor;
import ch.obya.pta.booking.domain.vo.InstructorId;
import ch.obya.pta.booking.domain.vo.Venue;
import ch.obya.pta.booking.domain.vo.VenueId;

/**
 * Instructors and venues, read from the database.
 */
@WithSession
@ApplicationScoped
public class PanacheRegistryAdapter implements InstructorRepository, VenueRepository {

    @Override
    public Uni<Instructor> findOne(InstructorId id) {
        return PanacheInstructorEntity.<PanacheInstructorEntity>find("logicalId", id.id()).firstResult()
                .onItem().ifNotNull().transform(PanacheInstructorEntity::toDomain);
    }

    @Override
    public Uni<List<Instructor>> findAll() {
        return PanacheInstructorEntity.<PanacheInstructorEntity>listAll()
                .map(l -> l.stream().map(PanacheInstructorEntity::toDomain).toList());
    }

    @Override
    public Uni<Venue> findOne(VenueId id) {
        return PanacheVenueEntity.<PanacheVenueEntity>find("logicalId", id.id()).firstResult()
                .onItem().ifNotNull().transform(PanacheVenueEntity::toDomain);
    }
}
