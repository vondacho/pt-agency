package ch.obya.pta.customer.domain;

import ch.obya.pta.common.util.validation.Checker;
import ch.obya.pta.customer.domain.event.CustomerCreated;
import ch.obya.pta.customer.domain.event.CustomerModified;
import ch.obya.pta.customer.domain.util.Samples;
import ch.obya.pta.customer.domain.vo.EmailAddress;
import ch.obya.pta.customer.domain.vo.Person;
import ch.obya.pta.customer.domain.vo.PhoneNumber;
import ch.obya.pta.customer.domain.vo.PhysicalAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static ch.obya.pta.customer.domain.util.Samples.johnDoe;
import static ch.obya.pta.customer.domain.util.Samples.sherlockHolmes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomerTest {

    @Test
    void should_create_customer_and_apply_fast_checks_and_emit_events() {
        var johnDoe = Samples.johnDoe();
        var state = johnDoe.state();

        assertThat(state.person().firstName().content()).isEqualTo("John");
        assertThat(state.person().lastName().content()).isEqualTo("Doe");
        assertThat(state.person().birth().date()).isEqualTo(LocalDate.of(1966, 4, 5));
        assertThat(state.person().gender()).isEqualTo(Person.Gender.MALE);
        assertThat(state.emailAddress()).isEqualTo(new EmailAddress("john@doe.ch"));
        assertThat(state.phoneNumber()).isEqualTo(new PhoneNumber("+41 78 123 45 67"));
        assertThat(state.deliveryAddress().streetNo()).isEqualTo("test-delivery");
        assertThat(state.billingAddress().streetNo()).isEqualTo("test-billing");
        assertThat(state.notes()).isEqualTo("no notes");

        assertThat(johnDoe.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new CustomerCreated(johnDoe.id()));
        assertThat(johnDoe.domainEvents()).isEmpty();
    }

    @Test
    void should_apply_modifications_and_apply_fast_checks_and_emit_events() {
        var customer = johnDoe();
        var sherlockHolmes = sherlockHolmes().state();

        customer.modify()
                .rename(sherlockHolmes.person().firstName(), sherlockHolmes.person().lastName())
                .redefine(sherlockHolmes.person().birth(), sherlockHolmes.person().gender())
                .reconnect(sherlockHolmes.emailAddress(), sherlockHolmes.phoneNumber())
                .relocate(sherlockHolmes.deliveryAddress(), sherlockHolmes.billingAddress())
                .annotate("modified by Sherlock Holmes")
                .done();

        var state = customer.state();

        assertThat(state.person().firstName().content()).isEqualTo(sherlockHolmes.person().firstName().content());
        assertThat(state.person().lastName().content()).isEqualTo(sherlockHolmes.person().lastName().content());
        assertThat(state.person().birth().date()).isEqualTo(sherlockHolmes.person().birth().date());
        assertThat(state.person().gender()).isEqualTo(sherlockHolmes.person().gender());
        assertThat(state.emailAddress().address()).isEqualTo(sherlockHolmes.emailAddress().address());
        assertThat(state.phoneNumber().number()).isEqualTo(sherlockHolmes.phoneNumber().number());
        assertThat(state.deliveryAddress().streetNo()).isEqualTo(sherlockHolmes.deliveryAddress().streetNo());
        assertThat(state.billingAddress().streetNo()).isEqualTo(sherlockHolmes.billingAddress().streetNo());
        assertThat(state.notes()).isEqualTo("modified by Sherlock Holmes");

        assertThat(customer.domainEvents())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(
                        new CustomerCreated(customer.id()),
                        new CustomerModified(customer.id()));
        assertThat(customer.domainEvents()).isEmpty();
    }

    @Test
    void should_apply_given_address_checker_on_actual_state() {
        var customer = Samples.johnDoe();

        var checker = mock(TestAddressChecker.class);
        customer.validateWith(checker);
        var captor = ArgumentCaptor.forClass(PhysicalAddress.class);
        verify(checker, times(2)).check(captor.capture());
        assertThat(captor.getAllValues()).extracting(PhysicalAddress::streetNo).containsExactly("test-delivery", "test-billing");
    }

    @Test
    void test() {
        LocalDate.parse("1958-12-12", DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private interface TestAddressChecker extends Checker<PhysicalAddress> {}
}
