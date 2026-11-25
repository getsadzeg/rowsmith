package com.example.lending.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily job: schedules the next automatic payment attempt after a failed one.
 * Rules: only the latest attempt of an installment counts and it must be FAILED with a soft decline
 * (hard declines ACCOUNT_CLOSED and STOLEN_CARD are never retried); at most MAX_ATTEMPTS attempts per
 * installment; the installment is still open (UNPAID or PARTIALLY_PAID) on an ACTIVE loan; loans with a
 * collection case that is not CLOSED are left to the collections team.
 * The retry charges the customer's current default payment method (ACTIVE) for the installment's
 * outstanding amount, RETRY_DELAY_DAYS after the run date. Customers without a default method are skipped.
 */
@Service
public class PaymentRetryService {

    public static final int MAX_ATTEMPTS = 3;
    public static final int RETRY_DELAY_DAYS = 3;

    private final NamedParameterJdbcTemplate jdbc;

    public PaymentRetryService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns the number of attempts scheduled. */
    @Transactional
    public int scheduleRetries(LocalDate today) {
        List<Map<String, Object>> failed = jdbc.queryForList("""
                SELECT a.installment_id, a.attempt_no, a.payment_method_id, i.outstanding, l.customer_id
                FROM payment_attempt a
                JOIN installment i ON i.id = a.installment_id
                JOIN loan l ON l.id = i.loan_id
                WHERE a.status = 'FAILED'
                  AND a.attempt_no = (SELECT max(x.attempt_no) FROM payment_attempt x WHERE x.installment_id = a.installment_id)
                  AND a.attempt_no < :maxAttempts
                  AND a.failure_code NOT IN ('ACCOUNT_CLOSED', 'STOLEN_CARD')
                  AND i.status IN ('UNPAID', 'PARTIALLY_PAID')
                  AND l.status = 'ACTIVE'
                  AND NOT EXISTS (SELECT 1 FROM collection_case c WHERE c.loan_id = l.id AND c.status <> 'CLOSED')
                ORDER BY a.id
                """, Map.of("maxAttempts", MAX_ATTEMPTS));
        int scheduled = 0;
        for (Map<String, Object> row : failed) {
            List<Long> method = jdbc.queryForList("""
                    SELECT id FROM payment_method WHERE customer_id = :customer AND is_default AND status = 'ACTIVE'
                    """, Map.of("customer", row.get("customer_id")), Long.class);
            if (method.isEmpty()) {
                continue;
            }
            Long methodId = method.get(0);
            jdbc.update("""
                    INSERT INTO payment_attempt (installment_id, payment_method_id, attempt_no, amount, status, scheduled_for)
                    VALUES (:installment, :method, :attemptNo, :amount, 'SCHEDULED', :when)
                    """, new MapSqlParameterSource().addValue("installment", row.get("installment_id"))
                    .addValue("method", methodId).addValue("attemptNo", ((Number) row.get("attempt_no")).intValue() + 1)
                    .addValue("amount", row.get("outstanding")).addValue("when", today.plusDays(RETRY_DELAY_DAYS)));
            scheduled++;
        }
        return scheduled;
    }
}
