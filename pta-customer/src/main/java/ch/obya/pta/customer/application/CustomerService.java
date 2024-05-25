package ch.obya.pta.customer.application;

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.util.EntityFinder;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.util.search.FindCriteria;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.event.CustomerRemoved;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.util.CustomerProblem;
import ch.obya.pta.customer.domain.vo.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;

@ApplicationScoped
public class CustomerService {

    @Inject
    CustomerRepository repository;
    @Inject
    EventPublisher eventPublisher;

    @Transactional
    public Uni<CustomerId> create(Person person,
                                  PhysicalAddress deliveryAddress,
                                  PhysicalAddress billingAddress,
                                  EmailAddress emailAddress,
                                  PhoneNumber phoneNumber,
                                  String notes) {

        return checkUniqueness(null, person, emailAddress).replaceWith(
                Uni.createFrom().item(
                    new Customer(
                            person,
                            deliveryAddress,
                            billingAddress,
                            emailAddress,
                            phoneNumber,
                            notes
                    ))
                .flatMap(c -> repository.save(c))
                .flatMap(c -> eventPublisher.publish(c.domainEvents()).replaceWith(c))
                .map(Customer::id));
    }

    @Transactional
    public Uni<Void> modify(CustomerId customerId, Consumer<Customer.Modifier> patches) {
        return findOne(customerId)
                .map(c -> {
                    var modifier = c.modify();
                    patches.accept(modifier);
                    modifier.done();
                    return c;
                })
                .flatMap(c -> {
                    var state = c.state();
                    return checkUniqueness(customerId, state.person(), state.emailAddress()).replaceWith(c);
                })
                .flatMap(c -> repository.save(c))
                .flatMap(c -> eventPublisher.publish(c.domainEvents()));
    }

    private Uni<Void> checkUniqueness(CustomerId exclude, Person person, EmailAddress emailAddress) {
        return repository.findByCriteria(FindCriteria.from("""
                    firstName:%s,
                    lastName:%s,
                    birthDate:%s
                    emailAddress:%s
                    """.formatted(person.firstName(), person.lastName(), person.birthDate(), emailAddress.address())))
                .select().where(c -> !c.id().equals(exclude))
                .onItem().failWith(CustomerProblem.AlreadyExisting.toException(
                            person.firstName(), person.lastName(), person.birthDate(), emailAddress))
                .ifNoItem().after(Duration.ofMillis(100))
                .recoverWithCompletion().toUni().replaceWithVoid();
    }

    @Transactional
    public Uni<Void> remove(CustomerId customerId) {
        return findOne(customerId)
                .flatMap(c -> repository.remove(customerId))
                .flatMap(c -> eventPublisher.publish(Set.of(new CustomerRemoved(customerId))));
    }

    public Uni<Customer> findOne(CustomerId id) {
        return EntityFinder.find(Customer.class, id, repository::findOne, CommonProblem.EntityNotFound);
    }
}