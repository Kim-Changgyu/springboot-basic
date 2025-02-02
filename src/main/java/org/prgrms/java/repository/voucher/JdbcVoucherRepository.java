package org.prgrms.java.repository.voucher;

import org.prgrms.java.domain.voucher.Voucher;
import org.prgrms.java.domain.voucher.VoucherType;
import org.prgrms.java.exception.badrequest.VoucherBadRequestException;
import org.prgrms.java.service.mapper.VoucherMapper;
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
public class JdbcVoucherRepository implements VoucherRepository {
    private static final String INSERT_QUERY = "INSERT INTO vouchers(voucher_id, owner_id, amount, type, created_at, expired_at, used) VALUES (UUID_TO_BIN(:voucherId), UUID_TO_BIN(:ownerId), :amount, :type, :createdAt, :expiredAt, :used)";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM vouchers WHERE voucher_id = UUID_TO_BIN(:voucherId)";
    private static final String FIND_BY_OWNER_QUERY = "SELECT * FROM vouchers WHERE owner_id = UUID_TO_BIN(:ownerId)";
    private static final String FIND_EXPIRED_VOUCHER_QUERY = "SELECT * FROM vouchers WHERE expired_at < CURRENT_TIMESTAMP";
    private static final String FIND_ALL_QUERY = "SELECT * FROM vouchers";
    private static final String UPDATE_QUERY = "UPDATE vouchers SET owner_id = UUID_TO_BIN(:ownerId), amount = :amount, type = :type, expired_at = :expiredAt, used = :used WHERE voucher_id = UUID_TO_BIN(:voucherId)";
    private static final String DELETE_QUERY = "DELETE FROM vouchers WHERE voucher_id = UUID_TO_BIN(:voucherId)";
    private static final String DELETE_ALL_ROWS_QUERY = "DELETE FROM vouchers";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcVoucherRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Optional<Voucher> findById(UUID voucherId) {
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    FIND_BY_ID_QUERY,
                    Collections.singletonMap("voucherId", voucherId.toString().getBytes()),
                    mapToVoucher));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Voucher> findByCustomer(UUID customerId) {
        return namedParameterJdbcTemplate.query(
                FIND_BY_OWNER_QUERY,
                Collections.singletonMap("ownerId", customerId.toString().getBytes()),
                mapToVoucher);
    }

    @Override
    public List<Voucher> findExpiredVouchers() {
        return namedParameterJdbcTemplate.query(FIND_EXPIRED_VOUCHER_QUERY, Collections.emptyMap(), mapToVoucher);
    }

    @Override
    public List<Voucher> findAll() {
        return namedParameterJdbcTemplate.query(FIND_ALL_QUERY, Collections.emptyMap(), mapToVoucher);
    }

    @Override
    public Voucher insert(Voucher voucher) {
        try {
            int result = namedParameterJdbcTemplate.update(INSERT_QUERY, toParamMap(voucher));
            if (result != 1) {
                throw new VoucherBadRequestException("바우처 생성 과정에서 문제가 발생했습니다.");
            }
            return voucher;
        } catch (DuplicateKeyException e) {
            throw new VoucherBadRequestException("이미 존재하는 아이디입니다.");
        }
    }

    @Override
    public Voucher update(Voucher voucher) {
        int result = namedParameterJdbcTemplate.update(UPDATE_QUERY, toParamMap(voucher));
        if (result != 1) {
            throw new VoucherBadRequestException("바우처 수정 과정에서 문제가 발생했습니다.");
        }
        return voucher;
    }

    @Override
    public void delete(UUID voucherId) {
        int result = namedParameterJdbcTemplate.update(DELETE_QUERY, Collections.singletonMap("voucherId", voucherId.toString().getBytes()));
        if (result != 1) {
            throw new VoucherBadRequestException("바우처 삭제 과정에서 문제가 발생했습니다.");
        }
    }

    @Override
    public void deleteAll() {
        namedParameterJdbcTemplate.update(DELETE_ALL_ROWS_QUERY, Collections.emptyMap());
    }


    private static Map<String, Object> toParamMap(Voucher voucher) {
        return new HashMap<>() {{
            put("voucherId", voucher.getVoucherId().toString().getBytes());
            put("ownerId", voucher.getOwnerId() == null? null: voucher.getOwnerId().toString().getBytes());
            put("amount", voucher.getAmount());
            put("type", voucher.getType().toString());
            put("createdAt", (voucher.getCreatedAt()));
            put("expiredAt", voucher.getExpiredAt());
            put("used", voucher.isUsed());
        }};
    }

    private static final RowMapper<Voucher> mapToVoucher = (resultSet, rowNum) -> {
        UUID voucherId = toUUID(resultSet.getBytes("voucher_id"));
        UUID customerId = resultSet.getBytes("owner_id") == null? null: toUUID(resultSet.getBytes("owner_id"));
        long amount = resultSet.getLong("amount");
        VoucherType type = VoucherType.of(resultSet.getString("type"));
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime expiredAt = resultSet.getTimestamp("expired_at").toLocalDateTime();
        boolean used = resultSet.getBoolean("used");

        return VoucherMapper.mapToVoucher(type, voucherId, customerId, amount, createdAt, expiredAt, used);
    };
}
