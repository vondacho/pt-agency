package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.util.exception.CommonProblem;
import ch.obya.pta.customer.domain.CustomerProblem;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

import static ch.obya.pta.common.util.exception.CommonProblem.*;

@Builder(builderClassName = "Builder", toBuilder = true)
public record Person(
        String salutation,
        Name firstName,
        Name lastName,
        BirthDate birthDate,
        Gender gender
        ) {

    public Person {
        ifNullThrow(lastName, CommonProblem.AttributeNotNull.toException("Person.lastName"));
        ifNullThrow(birthDate, CommonProblem.AttributeNotNull.toException("Person.birthDate"));
        ifNullThrow(gender, CommonProblem.AttributeNotNull.toException("Person.gender"));
    }

    public String fullName() {
        return Optional.ofNullable(firstName).map(fn -> "%s %s".formatted(fn, lastName.content)).orElse(lastName.content);
    }

    public record Name(String content) {
        public Name {
            ifNullThrow(content, CommonProblem.AttributeNotNull.toException("Name.content"));
            ifEmptyThrow(content, CommonProblem.AttributeNotEmpty.toException("Name.content"));
        }

        public Name standardize() {
            var contentLowerCase = content.toLowerCase();
            return new Name(contentLowerCase.substring(0, 1).toUpperCase() + contentLowerCase.substring(1));
        }
    }

    public record BirthDate(LocalDate date) {
        public BirthDate {
            ifNullThrow(date, CommonProblem.AttributeNotNull.toException("BirthDate.date"));
            ifThrow(() -> date.isAfter(LocalDate.now()), CustomerProblem.BirthDateInvalid.toException(date));
        }
    }

    public enum Gender { MALE, FEMALE }
}
