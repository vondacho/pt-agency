package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.customer.domain.util.CustomerProblem;
import lombok.Builder;

import java.text.DateFormat;
import java.time.LocalDate;

import static ch.obya.pta.common.domain.util.CommonProblem.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@Builder(builderClassName = "Builder", toBuilder = true)
public record Person(
        String salutation,
        Name firstName,
        Name lastName,
        Birth birth,
        Gender gender
        ) {

    public Person {
        ifNullThrow(firstName, CommonProblem.AttributeNotNull.toException("Person.firstName"));
        ifNullThrow(lastName, CommonProblem.AttributeNotNull.toException("Person.lastName"));
        ifNullThrow(birth, CommonProblem.AttributeNotNull.toException("Person.birth"));
        ifNullThrow(gender, CommonProblem.AttributeNotNull.toException("Person.gender"));
    }

    public String fullName() {
        return "%s %s".formatted(firstName.content(), lastName.content());
    }

    public record Birth(LocalDate date) {
        public Birth {
            ifNullThrow(date, CommonProblem.AttributeNotNull.toException("birth.date"));
            ifThrow(() -> date.isAfter(LocalDate.now()), CustomerProblem.BirthDateInvalid.toException(date));
        }

        public static Birth fromISO(String s) {
            return new Birth(LocalDate.parse(s, ISO_LOCAL_DATE));
        }

        public String toISO() {
            return ISO_LOCAL_DATE.format(date);
        }
    }

    public enum Gender { MALE, FEMALE }
}
