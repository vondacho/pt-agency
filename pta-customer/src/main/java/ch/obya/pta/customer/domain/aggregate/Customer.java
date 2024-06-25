package ch.obya.pta.customer.domain.aggregate;

import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.util.validation.Checker;
import ch.obya.pta.customer.domain.util.CustomerProblem;
import ch.obya.pta.customer.domain.entity.Subscription;
import ch.obya.pta.customer.domain.event.CustomerCreated;
import ch.obya.pta.customer.domain.event.CustomerModified;
import ch.obya.pta.customer.domain.util.PhoneNumberChecker;
import ch.obya.pta.customer.domain.vo.*;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public class Customer extends BaseEntity<Customer, CustomerId, Customer.State> {

    private final Set<Subscription> subscriptions = new HashSet<>();

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
            ifNullThrow(person, CommonProblem.AttributeNotNull.toException("Customer.email"));
            ifNullThrow(person, CommonProblem.AttributeNotNull.toException("Customer.phone"));
        }
    }

    public Customer(CustomerId id, Person person, PhysicalAddress deliveryAddress, PhysicalAddress billingAddress, EmailAddress emailAddress, PhoneNumber phoneNumber, String notes) {
        super(id, new State(person, deliveryAddress, billingAddress, emailAddress, phoneNumber, notes));
        state = validate(state);
        andEvent(new CustomerCreated(id));
    }

    public Customer(Person person, PhysicalAddress deliveryAddress, PhysicalAddress billingAddress, EmailAddress emailAddress, PhoneNumber phoneNumber, String notes) {
        this(CustomerId.create(), person, deliveryAddress, billingAddress, emailAddress, phoneNumber, notes);
    }

    public Set<Subscription> subscriptions() {
        return Set.copyOf(subscriptions);
    }

    public Set<Subscription> subscriptionsAt(LocalDate at) {
        return subscriptions.stream()
                .filter(s -> at == null || s.state().validity().includes(at))
                .collect(Collectors.toSet());
    }

    @Override
    protected State cloneState() {
        return state.toBuilder().build();
    }

    public Customer close() {
        return this;
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

    public void add(Subscription other) {
        this.subscriptions.stream()
                .filter(s -> s.overlappedBy(other))
                .findFirst()
                .ifPresent(overlapped -> {
                    throw CustomerProblem.SubscriptionOverlapsExisting.toException(other.id(), overlapped.id()).get();
                });
        this.subscriptions.add(other);
    }

    public Subscription subscriptionAt(SubscriptionId subscriptionId) {
        return subscriptions.stream()
                .filter(s -> s.id().equals(subscriptionId))
                .findFirst()
                .orElseThrow(CommonProblem.EntityNotFound.toException("Subscription", subscriptionId));
    }

    public Customer.Modifier modify() {
        return this.new Modifier();
    }

    public class Modifier {
        private final State.Builder stateBuilder = state.toBuilder();
        private final Person.Builder personBuilder = state.person.toBuilder();

        public Modifier rename(Name firstName, Name lastName) {
            if (firstName != null) personBuilder.firstName(firstName.standardize());
            if (lastName != null) personBuilder.lastName(lastName.standardize());
            return this;
        }

        public Modifier redefine(Person.Birth birth, Person.Gender gender) {
            if (birth != null) personBuilder.birth(birth);
            if (gender != null) personBuilder.gender(gender);
            return this;
        }

        public Modifier relocate(PhysicalAddress delivery, PhysicalAddress billing) {
            if (delivery != null) stateBuilder.deliveryAddress(delivery);
            if (billing != null) stateBuilder.billingAddress(billing);
            return this;
        }

        public Modifier reconnect(EmailAddress email, PhoneNumber phone) {
            if (email != null) stateBuilder.emailAddress(emailAddressChecker.check(email));
            if (phone != null) stateBuilder.phoneNumber(phoneNumberChecker.check(phone));
            return this;
        }

        public Modifier annotate(String notes) {
            stateBuilder.notes(notes);
            return this;
        }

        public void done() {
            state = stateBuilder.person(personBuilder.build()).build();
            andEvent(new CustomerModified(id));
        }
    }

    private static final Checker<EmailAddress> emailAddressChecker = new EmailAddress.EmailAddressChecker();
    private static final Checker<PhoneNumber> phoneNumberChecker = new PhoneNumberChecker();
    private static final Checker<PhysicalAddress> physicalAddressChecker = input -> input;
}
