package ch.obya.pta.customer.util.search;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.customer.domain.vo.EmailAddress;
import ch.obya.pta.customer.domain.vo.Person;
import ch.obya.pta.customer.domain.vo.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerAttributeFilterTest {

    @Test
    void should_build_attribute_filter() {
        assertThat(AttributeFilter.from("eq:albert", Name::new)).isEqualTo(AttributeFilter.equal(new Name("albert")));
        assertThat(AttributeFilter.from("eq:1958-12-12", Person.Birth::fromISO)).isEqualTo(AttributeFilter.equal(new Person.Birth(LocalDate.of(1958,12,12))));
        assertThat(AttributeFilter.from("eq:MALE", Person.Gender::valueOf)).isEqualTo(AttributeFilter.equal(Person.Gender.MALE));
        assertThat(AttributeFilter.from("eq:albert@einstein.ch", EmailAddress::new)).isEqualTo(AttributeFilter.equal(new EmailAddress("albert@einstein.ch")));
        assertThat(AttributeFilter.from("eq:0041781234567", PhoneNumber::new)).isEqualTo(AttributeFilter.equal(new PhoneNumber("0041781234567")));
    }
}
