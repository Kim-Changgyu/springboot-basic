package org.prgrms.java.domain.voucher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CreateVoucherRequest {
    private final String ownerId;
    private final long amount;
    private final String type;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private final LocalDateTime expiredAt;
}
