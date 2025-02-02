package org.prgrms.java.repository.customer;

import org.prgrms.java.domain.customer.Customer;
import org.prgrms.java.exception.badrequest.CustomerBadRequestException;
import org.prgrms.java.exception.notfound.CustomerNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemoryCustomerRepository implements CustomerRepository {
    private final Map<UUID, Customer> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Customer> findById(UUID customerId) {
        return Optional.ofNullable(storage.get(customerId));
    }

    @Override
    public Optional<Customer> findByName(String name) {
        return storage.values().stream()
                .filter(customer -> customer.getName().equals(name))
                .findAny();
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return storage.values().stream()
                .filter(customer -> customer.getEmail().equals(email))
                .findAny();
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public Customer save(Customer customer) {
        if (findById(customer.getCustomerId()).isPresent()) {
            throw new CustomerBadRequestException("이미 존재하는 아이디입니다.");
        }
        storage.put(customer.getCustomerId(), customer);
        return storage.get(customer.getCustomerId());
    }

    @Override
    public Customer update(Customer customer) {
        if (findById(customer.getCustomerId()).isEmpty()) {
            throw new CustomerNotFoundException();
        }
        storage.put(customer.getCustomerId(), customer);
        return storage.get(customer.getCustomerId());
    }

    @Override
    public void delete(UUID customerId) {
        if (findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException();
        }
        storage.remove(customerId);
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
