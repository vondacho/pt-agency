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

import java.util.Set;
import java.util.function.Consumer;

@ApplicationScoped
public class CustomerService {

    @Inject
    CustomerRepository customerRepository;
    @Inject
    EventPublisher eventPublisher;

    @Transactional
    public Uni<CustomerId> create(Person person,
                                  PhysicalAddress deliveryAddress,
                                  PhysicalAddress billingAddress,
                                  EmailAddress emailAddress,
                                  PhoneNumber phoneNumber,
                                  String notes) {

        return checkUniqueness(person, emailAddress).replaceWith(
                Uni.createFrom().item(
                    new Customer(
                            person,
                            deliveryAddress,
                            billingAddress,
                            emailAddress,
                            phoneNumber,
                            notes
                    ))
                .flatMap(c -> customerRepository.save(c))
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
                    return checkUniqueness(state.person(), state.emailAddress()).replaceWith(c);
                })
                .flatMap(c -> customerRepository.save(c))
                .flatMap(c -> eventPublisher.publish(c.domainEvents()));
    }

    private Uni<Void> checkUniqueness(Person person, EmailAddress emailAddress) {
        return customerRepository.findByCriteria(
                FindCriteria.from("""
                    firstName:%s,
                    lastName:%s,
                    birthDate:%s
                    emailAddress:%s
                    """.formatted(person.firstName(), person.lastName(), person.birthDate(), emailAddress.address())))
                .invoke(result -> {
                    if (!result.isEmpty()) {
                        throw CustomerProblem.AlreadyExisting.toException(
                            person.firstName(), person.lastName(), person.birthDate(), emailAddress).get();
                    }
                }).replaceWithVoid();
    }

    @Transactional
    public Uni<Void> remove(CustomerId customerId) {
        return findOne(customerId)
                .flatMap(c -> customerRepository.remove(customerId))
                .invoke(() -> eventPublisher.publish(Set.of(new CustomerRemoved(customerId))));
    }

    public Uni<Customer> findOne(CustomerId id) {
        return EntityFinder.find(Customer.class, id, customerRepository::findOne, CommonProblem.EntityNotFound);
    }
}