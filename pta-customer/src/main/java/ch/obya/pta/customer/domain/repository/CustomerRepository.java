package ch.obya.pta.customer.domain.repository;

import ch.obya.pta.common.domain.repository.EntityRepository;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.CustomerId;

public interface CustomerRepository extends EntityRepository<Customer, CustomerId, Customer.State> {
}

