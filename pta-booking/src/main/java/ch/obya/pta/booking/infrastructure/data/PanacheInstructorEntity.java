package ch.obya.pta.booking.infrastructure.data;

import java.util.UUID;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import ch.obya.pta.booking.domain.vo.Instructor;
import ch.obya.pta.booking.domain.vo.InstructorId;

@Entity(name = "instructor")
class PanacheInstructorEntity extends PanacheEntity {
    UUID logicalId;
    String name;
    String bio;

    Instructor toDomain() {
        return new Instructor(new InstructorId(logicalId), name, bio);
    }
}
