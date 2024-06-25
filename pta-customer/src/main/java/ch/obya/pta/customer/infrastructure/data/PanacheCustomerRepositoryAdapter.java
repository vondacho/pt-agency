package ch.obya.pta.customer.infrastructure.data;

import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.vo.CustomerId;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@WithTransaction
@ApplicationScoped
public class PanacheCustomerRepositoryAdapter implements CustomerRepository {

    @Override
    public Uni<Customer> findOne(CustomerId id) {
        return PanacheCustomerEntity.findByLogicalId(id)
                .firstResult()
                .map(PanacheCustomerEntity::toDomain);
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

    @Override
    public Uni<CustomerId> save(CustomerId id, Customer.State state) {
        return PanacheCustomerEntity.findByLogicalId(id)
                .firstResult()
                .ifNoItem().after(Duration.ofMillis(100)).recoverWithUni(() -> Uni.createFrom().item(new PanacheCustomerEntity()))
                .replaceIfNullWith(new PanacheCustomerEntity())
                .map(e -> e.set(id, state))
                .flatMap(e -> e.persist())
                .replaceWith(id);
    }

    @Override
    public Uni<Void> remove(CustomerId id) {
        return PanacheCustomerEntity.delete("logicalId", id.id()).replaceWithVoid();
    }
}
