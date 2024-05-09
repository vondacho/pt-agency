package ch.obya.pta.customer.domain;

import ch.obya.pta.common.util.validation.Checker;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.EmailAddress;
import ch.obya.pta.customer.domain.vo.Person;
import ch.obya.pta.customer.domain.vo.PhoneNumber;
import ch.obya.pta.customer.domain.vo.PhysicalAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CustomerTest {

    @Test
    void createThenModifyThenValidate() {
        var customer = new Customer(
            new Person("Mr", new Person.Name("john"), new Person.Name("doe"), new Person.BirthDate(LocalDate.of(1966, 4, 5)), Person.Gender.MALE),
            new PhysicalAddress("test-delivery", "zip", "city", "region", "ch"),
            new PhysicalAddress("test-billing", "zip", "city", "region", "ch"),
            new EmailAddress("john@doe.ch"),
            new PhoneNumber("0041781234567"),"notes");

        var state = customer.state();

        assertThat(state.person().firstName().content()).isEqualTo("John");
        assertThat(state.person().lastName().content()).isEqualTo("Doe");
        assertThat(state.person().birthDate().date()).isEqualTo(LocalDate.of(1966, 4, 5));
        assertThat(state.person().gender()).isEqualTo(Person.Gender.MALE);
        assertThat(state.emailAddress()).isEqualTo(new EmailAddress("john@doe.ch"));
        assertThat(state.phoneNumber()).isEqualTo(new PhoneNumber("+41 78 123 45 67"));
        assertThat(state.deliveryAddress().streetNo()).isEqualTo("test-delivery");
        assertThat(state.billingAddress().streetNo()).isEqualTo("test-billing");
        assertThat(state.notes()).isEqualTo("notes");

        customer.modify()
                .rename(new Person.Name("albert"), new Person.Name("einstein"))
                .redefine(new Person.BirthDate(LocalDate.of(1958, 12,12)), Person.Gender.MALE)
                .reconnect(new EmailAddress("albert@einstein.com"), new PhoneNumber("+33123456789"))
                .relocate(new PhysicalAddress("Bakerstreet 22", "89000", "London", "London", "UK"), state.deliveryAddress())
                .annotate("modified")
                .done();

        state = customer.state();

        assertThat(state.person().firstName().content()).isEqualTo("Albert");
        assertThat(state.person().lastName().content()).isEqualTo("Einstein");
        assertThat(state.person().birthDate().date()).isEqualTo(LocalDate.of(1958, 12,12));
        assertThat(state.person().gender()).isEqualTo(Person.Gender.MALE);
        assertThat(state.emailAddress()).isEqualTo(new EmailAddress("albert@einstein.com"));
        assertThat(state.phoneNumber()).isEqualTo(new PhoneNumber("+33 1 23 45 67 89"));
        assertThat(state.deliveryAddress().streetNo()).isEqualTo("Bakerstreet 22");
        assertThat(state.billingAddress().streetNo()).isEqualTo("test-delivery");
        assertThat(state.notes()).isEqualTo("modified");

        var checker = mock(TestAddressChecker.class);
        customer.validateWith(checker);
        var captor = ArgumentCaptor.forClass(PhysicalAddress.class);
        verify(checker, times(2)).check(captor.capture());
        assertThat(captor.getAllValues()).extracting(PhysicalAddress::streetNo).containsExactly("Bakerstreet 22", "test-delivery");
    }

    private interface TestAddressChecker extends Checker<PhysicalAddress> {}
}
