package ch.obya.pta.customer.domain.aggregate;

import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.util.exception.CommonProblem;
import ch.obya.pta.common.util.validation.Checker;
import ch.obya.pta.customer.domain.vo.*;
import lombok.AccessLevel;
import lombok.Builder;

import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;

public class Customer extends BaseEntity<Customer, CustomerId, Customer.State> {
    @Builder(builderClassName = "Builder", toBuilder = true, access = AccessLevel.PRIVATE)
    public record State(
        Person person,
        PhysicalAddress deliveryAddress,
        PhysicalAddress billingAddress,
        EmailAddress emailAddress,
        PhoneNumber phoneNumber,
        String notes) {

        public State {
            ifNullThrow(person, CommonProblem.AttributeNotNull.toException("Customer.person"));
            ifNullThrow(person, CommonProblem.AttributeNotNull.toException("Customer.emailAddress"));
            ifNullThrow(person, CommonProblem.AttributeNotNull.toException("Customer.phoneNumber"));
        }
    }

    public Customer(CustomerId id, Person person, PhysicalAddress deliveryAddress, PhysicalAddress billingAddress, EmailAddress emailAddress, PhoneNumber phoneNumber, String notes) {
        super(id, new State(person, deliveryAddress, billingAddress, emailAddress, phoneNumber, notes));
        state = validate(state);
    }

    public Customer(Person person, PhysicalAddress deliveryAddress, PhysicalAddress billingAddress, EmailAddress emailAddress, PhoneNumber phoneNumber, String notes) {
        this(CustomerId.create(), person, deliveryAddress, billingAddress, emailAddress, phoneNumber, notes);
    }

    @Override
    protected State validate(State state) {
        return state.toBuilder()
                .person(state.person.toBuilder()
                        .firstName(state.person.firstName().standardize())
                        .lastName(state.person.lastName().standardize())
                        .build()
                )
                .deliveryAddress(physicalAddressChecker.check(state.deliveryAddress))
                .billingAddress(physicalAddressChecker.check(state.billingAddress))
                .emailAddress(emailAddressChecker.check(state.emailAddress))
                .phoneNumber(phoneNumberChecker.check(state.phoneNumber))
                .build();
    }

    public void validateWith(Checker<PhysicalAddress> checker) {
        state = state.toBuilder()
                .deliveryAddress(checker.check(state.deliveryAddress))
                .billingAddress(checker.check(state.billingAddress))
                .build();
    }

    public Customer.Modifier modify() {
        return this.new Modifier();
    }

    public class Modifier {
        private final State.Builder stateBuilder = state.toBuilder();
        private final Person.Builder personBuilder = state.person.toBuilder();

        public Modifier rename(Person.Name firstName, Person.Name lastName) {
            personBuilder.firstName(firstName.standardize()).lastName(lastName.standardize());
            return this;
        }

        public Modifier redefine(Person.BirthDate birthDate, Person.Gender gender) {
            personBuilder.birthDate(birthDate).gender(gender);
            return this;
        }

        public Modifier relocate(PhysicalAddress deliveryAddress, PhysicalAddress billingAddress) {
            stateBuilder.deliveryAddress(deliveryAddress).billingAddress(billingAddress);
            return this;
        }

        public Modifier reconnect(EmailAddress emailAddress, PhoneNumber phoneNumber) {
            stateBuilder
                    .emailAddress(emailAddressChecker.check(emailAddress))
                    .phoneNumber(phoneNumberChecker.check(phoneNumber));
            return this;
        }

        public Modifier annotate(String notes) {
            stateBuilder.notes(notes);
            return this;
        }

        public void done() {
            state = stateBuilder.person(personBuilder.build()).build();
        }
    }

    private static final Checker<EmailAddress> emailAddressChecker = new EmailAddress.EmailAddressChecker();
    private static final Checker<PhoneNumber> phoneNumberChecker = new PhoneNumberChecker();
    private static final Checker<PhysicalAddress> physicalAddressChecker = input -> input;
}
