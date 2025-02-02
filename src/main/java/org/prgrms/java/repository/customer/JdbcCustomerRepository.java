package org.prgrms.java.repository.customer;

import org.prgrms.java.domain.customer.Customer;
import org.prgrms.java.exception.badrequest.CustomerBadRequestException;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

import static org.prgrms.java.common.TypeConversionUtils.toUUID;

@Repository
@Primary
public class JdbcCustomerRepository implements CustomerRepository {
    private static final String INSERT_QUERY = "INSERT INTO customers(customer_id, name, email, created_at, is_blocked) VALUES (UUID_TO_BIN(:customerId), :name, :email, :createdAt, :isBlocked)";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM customers WHERE customer_id = UUID_TO_BIN(:customerId)";
    private static final String FIND_BY_NAME_QUERY = "SELECT * FROM customers WHERE name = :name";
    private static final String FIND_BY_EMAIL_QUERY = "SELECT * FROM customers WHERE email = :email";
    private static final String FIND_ALL_QUERY = "SELECT * FROM customers";
    private static final String UPDATE_QUERY = "UPDATE customers SET name = :name, email = :email, is_blocked = :isBlocked WHERE customer_id = UUID_TO_BIN(:customerId)";
    private static final String DELETE_QUERY = "DELETE FROM customers WHERE customer_id = UUID_TO_BIN(:customerId)";
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
                    mapToCustomer));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Customer> findByName(String name) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    FIND_BY_NAME_QUERY,
                    Collections.singletonMap("name", name),
                    mapToCustomer));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    FIND_BY_EMAIL_QUERY,
                    Collections.singletonMap("email", email),
                    mapToCustomer));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Customer> findAll() {
        return namedParameterJdbcTemplate.query(FIND_ALL_QUERY, Collections.emptyMap(), mapToCustomer);
    }

    @Override
    public Customer save(Customer customer) {
        try {
            int result = namedParameterJdbcTemplate.update(INSERT_QUERY, toParamMap(customer));
            if (result != 1) {
                throw new CustomerBadRequestException("사용자 생성 과정에서 문제가 발생했습니다.");
            }
            return customer;
        } catch (DuplicateKeyException e) {
            throw new CustomerBadRequestException("이미 존재하는 아이디입니다.");
        }
    }

    @Override
    public Customer update(Customer customer) {
        int result = namedParameterJdbcTemplate.update(UPDATE_QUERY, toParamMap(customer));
        if (result != 1) {
            throw new CustomerBadRequestException("사용자 수정 과정에서 문제가 발생했습니다.");
        }
        return customer;
    }

    @Override
    public void delete(UUID customerId) {
        int result = namedParameterJdbcTemplate.update(DELETE_QUERY, Collections.singletonMap("customerId", customerId.toString().getBytes()));
        if (result != 1) {
            throw new CustomerBadRequestException("사용자 삭제 과정에서 문제가 발생했습니다.");
        }
    }

    @Override
    public void deleteAll() {
        namedParameterJdbcTemplate.update(DELETE_ALL_ROWS_QUERY, Collections.emptyMap());
    }

    private static final RowMapper<Customer> mapToCustomer = (resultSet, rowNum) -> {
        UUID customerId = toUUID(resultSet.getBytes("customer_id"));
        String name = resultSet.getString("name");
        String email = resultSet.getString("email");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        boolean isBlocked = resultSet.getBoolean("is_blocked");
        return Customer.builder()
                .customerId(customerId)
                .name(name)
                .email(email)
                .createdAt(createdAt)
                .isBlocked(isBlocked)
                .build();
    };

    private static Map<String, Object> toParamMap(Customer customer) {
        return new HashMap<>() {{
            put("customerId", customer.getCustomerId().toString().getBytes());
            put("name", customer.getName());
            put("email", customer.getEmail());
            put("createdAt", customer.getCreatedAt());
            put("isBlocked", customer.isBlocked());
        }};
    }
}
