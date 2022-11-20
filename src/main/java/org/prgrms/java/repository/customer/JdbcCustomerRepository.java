package org.prgrms.java.repository.customer;

import org.prgrms.java.domain.customer.Customer;
import org.prgrms.java.exception.CustomerException;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.util.*;

@Repository
@Primary
public class JdbcCustomerRepository implements CustomerRepository {
    private static final String INSERT_QUERY = "INSERT INTO customers(customer_id, name, email, is_blocked) VALUES (UUID_TO_BIN(:customerId), :name, :email, :isBlocked)";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM customers WHERE customer_id = UUID_TO_BIN(:customerId)";
    private static final String FIND_ALL_QUERY = "SELECT * FROM customers";
    private static final String UPDATE_QUERY = "UPDATE customers SET name = :name, email = :email, is_blocked = :isBlocked WHERE customer_id = UUID_TO_BIN(:customerId)";
    private static final String DELETE_ALL_ROWS_QUERY = "DELETE FROM customers";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcCustomerRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    FIND_BY_ID_QUERY,
                    Collections.singletonMap("customerId", customerId.toString().getBytes()),
                    customerRowMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<Customer> findAll() {
        return namedParameterJdbcTemplate.query(FIND_ALL_QUERY, Collections.emptyMap(), customerRowMapper);
    }

    @Override
    public Customer insert(Customer customer) {
        try {
            int result = namedParameterJdbcTemplate.update(INSERT_QUERY, toParamMap(customer));
            if (result != 1) {
                throw new CustomerException("Nothing was inserted.");
            }
            return customer;
        } catch (DuplicateKeyException e) {
            throw new CustomerException(customer.getCustomerId() + " is already exists.");
        }
    }

    @Override
    public Customer update(Customer customer) {
        int result = namedParameterJdbcTemplate.update(UPDATE_QUERY, toParamMap(customer));
        if (result != 1) {
            throw new CustomerException("Nothing was updated");
        }
        return customer;
    }

    @Override
    public long deleteAll() {
        return namedParameterJdbcTemplate.update(DELETE_ALL_ROWS_QUERY, Collections.emptyMap());
    }

    static UUID toUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }

    private Map<String, Object> toParamMap(Customer customer) {
        return new HashMap<>() {{
            put("customerId", customer.getCustomerId().toString().getBytes());
            put("name", customer.getName());
            put("email", customer.getEmail());
            put("isBlocked", customer.isBlocked());
        }};
    }

    private static final RowMapper<Customer> customerRowMapper = (resultSet, rowNum) -> {
        UUID customerId = toUUID(resultSet.getBytes("customer_id"));
        String customerName = resultSet.getString("name");
        String email = resultSet.getString("email");
        boolean isBlocked = resultSet.getBoolean("is_blocked");
        return new Customer(customerId, customerName, email, isBlocked);
    };
}
