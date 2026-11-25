package com.example.lending.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily job: assesses a late fee on installments that are past due beyond the grace period.
 * Rules: installment UNPAID or PARTIALLY_PAID, due date more than GRACE_DAYS before today, loan ACTIVE,
 * no active dispute (OPEN or UNDER_REVIEW) on the loan, and at most one fee per installment ever.
 * Fee: 5% of the outstanding amount, at least 5.00 and at most 25.00.
 */
@Service
public class LateFeeService {

    public static final int GRACE_DAYS = 10;
    static final BigDecimal MIN_FEE = new BigDecimal("5.00");
    static final BigDecimal MAX_FEE = new BigDecimal("25.00");
    static final BigDecimal RATE = new BigDecimal("0.05");

    private final NamedParameterJdbcTemplate jdbc;

    public LateFeeService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns the number of fees assessed. */
    @Transactional
    public int assessLateFees(LocalDate today) {
        List<Map<String, Object>> due = jdbc.queryForList("""
                SELECT i.id, i.outstanding
                FROM installment i
                JOIN loan l ON l.id = i.loan_id
                WHERE i.status IN ('UNPAID', 'PARTIALLY_PAID')
                  AND i.due_date < :cutoff
                  AND l.status = 'ACTIVE'
                  AND NOT EXISTS (SELECT 1 FROM dispute d WHERE d.loan_id = l.id AND d.status IN ('OPEN', 'UNDER_REVIEW'))
                  AND NOT EXISTS (SELECT 1 FROM late_fee f WHERE f.installment_id = i.id)
                ORDER BY i.id
                """, Map.of("cutoff", today.minusDays(GRACE_DAYS)));
        for (Map<String, Object> row : due) {
            jdbc.update("INSERT INTO late_fee (installment_id, amount, assessed_on) VALUES (:id, :amount, :today)",
                    Map.of("id", row.get("id"), "amount", fee((BigDecimal) row.get("outstanding")), "today", today));
        }
        return due.size();
    }

    static BigDecimal fee(BigDecimal outstanding) {
        BigDecimal fee = outstanding.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
        return fee.max(MIN_FEE).min(MAX_FEE);
    }
}
