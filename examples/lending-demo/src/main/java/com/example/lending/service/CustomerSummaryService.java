package com.example.lending.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What support agents see first: open loans, total outstanding, primary city, latest KYC status. */
@Service
public class CustomerSummaryService {

    public record Summary(long customerId, String fullName, long openLoans, BigDecimal outstanding, String primaryCity, String kycStatus) {}

    private final NamedParameterJdbcTemplate jdbc;

    public CustomerSummaryService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Summary summary(long customerId) {
        Map<String, Object> c = jdbc.queryForMap("SELECT first_name || ' ' || last_name AS name FROM customer WHERE id = :id", Map.of("id", customerId));
        Long open = jdbc.queryForObject("SELECT count(*) FROM loan WHERE customer_id = :id AND status = 'ACTIVE'", Map.of("id", customerId), Long.class);
        BigDecimal outstanding = jdbc.queryForObject("""
                SELECT coalesce(sum(i.amount - i.paid_amount), 0) FROM installment i JOIN loan l ON l.id = i.loan_id
                WHERE l.customer_id = :id AND l.status = 'ACTIVE' AND i.status IN ('UNPAID', 'PARTIALLY_PAID')
                """, Map.of("id", customerId), BigDecimal.class);
        List<String> city = jdbc.queryForList("SELECT city FROM address WHERE customer_id = :id AND is_primary", Map.of("id", customerId), String.class);
        List<String> kyc = jdbc.queryForList("SELECT status FROM kyc_check WHERE customer_id = :id ORDER BY checked_at DESC, id DESC LIMIT 1",
                Map.of("id", customerId), String.class);
        return new Summary(customerId, (String) c.get("name"), open, outstanding,
                city.isEmpty() ? null : city.get(0), kyc.isEmpty() ? null : kyc.get(0));
    }
}
