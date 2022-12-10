package org.prgrms.java.common;

import org.prgrms.java.domain.customer.Customer;
import org.prgrms.java.domain.voucher.FixedAmountVoucher;
import org.prgrms.java.domain.voucher.PercentDiscountVoucher;
import org.prgrms.java.domain.voucher.Voucher;
import org.prgrms.java.exception.badrequest.CustomerBadRequestException;
import org.prgrms.java.exception.badrequest.VoucherBadRequestException;
import org.prgrms.java.exception.notfound.CustomerNotFoundException;
import org.springframework.jdbc.core.RowMapper;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Mapper {
    private Mapper() {
    }

    public static Voucher mapToVoucher(String line) {
        String[] fields = line.split(",");
        if (fields.length != 7) {
            throw new VoucherBadRequestException("손상된 바우처 데이터입니다.");
        }

        UUID voucherId = UUID.fromString(fields[0].trim());
        UUID ownerId = (fields[1].trim().equals("null"))? null: UUID.fromString(fields[1].trim());
        String amount = fields[2].trim();
        String type = fields[3].trim();
        LocalDateTime createdAt = LocalDateTime.parse(fields[4].trim());
        LocalDateTime expiredAt = LocalDateTime.parse(fields[5].trim());
        boolean used = Boolean.parseBoolean(fields[6].trim());

        return mapToVoucher(type, voucherId, ownerId, Long.parseLong(amount), createdAt, expiredAt, used);
    }

    public static Voucher mapToVoucher(String type, UUID voucherId, UUID ownerId, long amount, LocalDateTime createdAt, LocalDateTime expiredAt, boolean used) {
        switch (type) {
            case "PERCENT" -> {
                return PercentDiscountVoucher.builder()
                        .voucherId(voucherId)
                        .ownerId(ownerId)
                        .amount(amount)
                        .isUsed(used)
                        .createdAt(createdAt)
                        .expiredAt(expiredAt)
                        .build();
            }
            case "FIXED" -> {
                return FixedAmountVoucher.builder()
                        .voucherId(voucherId)
                        .ownerId(ownerId)
                        .amount(amount)
                        .isUsed(used)
                        .createdAt(createdAt)
                        .expiredAt(expiredAt)
                        .build();
            }
            default -> throw new VoucherBadRequestException("잘못된 바우처 타입입니다.");
        }
    }

    public static Customer mapToCustomer(String line) {
        String[] fields = line.split(",");
        if (fields.length != 5) {
            throw new CustomerBadRequestException("손상된 사용자 데이터입니다.");
        }

        UUID customerId = UUID.fromString(fields[0].trim());
        String name = fields[1].trim();
        String email = fields[2].trim();
        LocalDateTime createdAt = LocalDateTime.parse(fields[3].trim());
        boolean isBlocked = Boolean.parseBoolean(fields[4].trim());

        return Customer.builder()
                .customerId(customerId)
                .name(name)
                .email(email)
                .createdAt(createdAt)
                .isBlocked(isBlocked)
                .build();
    }

    public static final RowMapper<Voucher> mapToVoucher = (resultSet, rowNum) -> {
        UUID voucherId = toUUID(resultSet.getBytes("voucher_id"));
        UUID customerId = resultSet.getBytes("owner_id") == null? null: toUUID(resultSet.getBytes("owner_id"));
        long amount = resultSet.getLong("amount");
        String type = resultSet.getString("type");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime expiredAt = resultSet.getTimestamp("expired_at").toLocalDateTime();
        boolean used = resultSet.getBoolean("used");

        return Mapper.mapToVoucher(type, voucherId, customerId, amount, createdAt, expiredAt, used);
    };

    public static final RowMapper<Customer> mapToCustomer = (resultSet, rowNum) -> {
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

    public static Map<String, Object> toParamMap(Voucher voucher) {
        return new HashMap<>() {{
            put("voucherId", voucher.getVoucherId().toString().getBytes());
            put("ownerId", voucher.getOwnerId() == null? null: voucher.getOwnerId().toString().getBytes());
            put("amount", voucher.getAmount());
            put("type", voucher.getType());
            put("createdAt", (voucher.getCreatedAt()));
            put("expiredAt", voucher.getExpiredAt());
            put("used", voucher.isUsed());
        }};
    }

    public static Map<String, Object> toParamMap(Customer customer) {
        return new HashMap<>() {{
            put("customerId", customer.getCustomerId().toString().getBytes());
            put("name", customer.getName());
            put("email", customer.getEmail());
            put("createdAt", customer.getCreatedAt());
            put("isBlocked", customer.isBlocked());
        }};
    }

    public static UUID toUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }
}
