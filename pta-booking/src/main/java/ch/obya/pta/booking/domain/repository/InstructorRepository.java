package ch.obya.pta.booking.domain.repository;

import java.util.List;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.vo.Instructor;
import ch.obya.pta.booking.domain.vo.InstructorId;

/** Instructors leading sessions (read-only for now: managed through the seed until a staff UI exists). */
public interface InstructorRepository {

    /** The instructor, or a null item when unknown. */
    Uni<Instructor> findOne(InstructorId id);

    Uni<List<Instructor>> findAll();
}
