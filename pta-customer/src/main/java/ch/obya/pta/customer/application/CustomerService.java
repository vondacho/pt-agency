package ch.obya.pta.customer.application;

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.util.EntityFinder;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.event.CustomerRemoved;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.util.CustomerProblem;
import ch.obya.pta.customer.domain.vo.*;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static ch.obya.pta.common.util.search.AttributeFilter.equal;
import static ch.obya.pta.common.util.search.AttributeFilter.notEqual;

@ApplicationScoped
public class CustomerService {

    @Inject
    CustomerRepository repository;
    @Inject
    EventPublisher eventPublisher;

    @WithTransaction
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
                        .flatMap(c -> repository.save(c).replaceWith(c))
                        .flatMap(c -> eventPublisher.publish(c.domainEvents()).replaceWith(c))
                        .map(Customer::id)
        );
    }

    @WithTransaction
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
                .flatMap(c -> repository.save(c).replaceWith(c))
                .flatMap(c -> eventPublisher.publish(c.domainEvents()));
    }

    private Uni<Void> checkUniqueness(@Nullable CustomerId exclude, Person person, EmailAddress emailAddress) {
        return repository.findByCriteria(exclude != null ?
                Map.of(
                        "id", List.of(notEqual(exclude)),
                        "firstName", List.of(equal(person.firstName())),
                        "lastName", List.of(equal(person.lastName())),
                        "birth", List.of(equal(person.birth())),
                        "email", List.of(equal(emailAddress))
                ): Map.of(
                        "firstName", List.of(equal(person.firstName())),
                        "lastName", List.of(equal(person.lastName())),
                        "birth", List.of(equal(person.birth())),
                        "email", List.of(equal(emailAddress))
                ))
                .flatMap(l -> l.isEmpty() ?
                        Uni.createFrom().voidItem() :
                        Uni.createFrom().failure(CustomerProblem.AlreadyExisting
                                .toException(person.firstName(), person.lastName(), person.birth(), emailAddress).get()));
    }

    @WithTransaction
    public Uni<Void> remove(CustomerId customerId, Boolean force) {
        return findOne(customerId)
                .flatMap(c -> (force ? repository.remove(customerId) : repository.save(c.close())).replaceWith(c))
                .flatMap(c -> eventPublisher.publish(force ? Set.of(new CustomerRemoved(customerId)) : c.domainEvents()));
    }

    @WithTransaction
    public Uni<Customer> findOne(CustomerId id) {
        return EntityFinder.find(Customer.class, id, repository::findOne, CommonProblem.EntityNotFound);
    }
}