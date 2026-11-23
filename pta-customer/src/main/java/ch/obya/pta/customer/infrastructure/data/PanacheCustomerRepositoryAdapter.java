package ch.obya.pta.customer.infrastructure.data;

import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.vo.CustomerId;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@WithTransaction
@ApplicationScoped
public class PanacheCustomerRepositoryAdapter implements CustomerRepository {

    @Override
    public Uni<Customer> findOne(CustomerId id) {
        return PanacheCustomerEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNotNull().transform(PanacheCustomerEntity::toDomain);
    }

    @Override
    public Uni<List<Customer>> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        return PanacheCustomerEntity.findByCriteria(criteria)
                .list()
                .map(l -> l.stream().map(PanacheCustomerEntity::toDomain).toList());
    }

    @Override
    public Uni<List<Customer>> findAll() {
        return PanacheCustomerEntity.<PanacheCustomerEntity>findAll()
                .list()
                .map(l -> l.stream().map(PanacheCustomerEntity::toDomain).toList());
    }

    /**
     * Creates or updates a customer together with its subscriptions, which are not part of {@link Customer.State}.
     */
    @Override
    public Uni<CustomerId> save(Entity<Customer, CustomerId, Customer.State> entity) {
        var customer = (Customer) entity;
        return PanacheCustomerEntity.findByLogicalId(customer.id())
                .firstResult()
                .replaceIfNullWith(PanacheCustomerEntity::new)
                .map(e -> e.set(customer))
                .flatMap(e -> e.persist())
                .replaceWith(customer.id());
    }

    /**
     * Updates the state of an existing customer; its subscriptions are left unchanged.
     */
    @Override
    public Uni<CustomerId> save(CustomerId id, Customer.State state) {
        return PanacheCustomerEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNull().failWith(CommonProblem.EntityNotFound.toException("Customer", id.id()))
                .map(e -> e.set(id, state))
                .flatMap(e -> e.persist())
                .replaceWith(id);
    }

    @Override
    public Uni<Void> remove(CustomerId id) {
        return PanacheCustomerEntity.findByLogicalId(id)
                .firstResult()
                .onItem().ifNotNull().transformToUni(e -> e.delete())
                .replaceWithVoid();
    }
}
