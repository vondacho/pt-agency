package ch.obya.pta.customer.infrastructure.data;

import ch.obya.pta.common.util.search.FindCriteria;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.vo.CustomerId;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;

@ApplicationScoped
public class DefaultCustomerRepository implements CustomerRepository {
    @Override
    public Uni<Customer> findOne(CustomerId id) {
        return null;
    }

    @Override
    public Multi<Customer> findByCriteria(Collection<FindCriteria> criteria) {
        return null;
    }

    @Override
    public Uni<Customer> save(CustomerId id, Customer.State state) {
        return null;
    }

    @Override
    public Uni<Void> remove(CustomerId id) {
        return null;
    }
}
