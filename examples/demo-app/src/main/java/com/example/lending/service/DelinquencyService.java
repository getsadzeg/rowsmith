package com.example.lending.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Charge-off candidates: ACTIVE loans with an installment more than 90 days past due and no active dispute. */
@Service
public class DelinquencyService {

    public static final int CHARGE_OFF_DAYS = 90;

    private final NamedParameterJdbcTemplate jdbc;

    public DelinquencyService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Long> chargeOffCandidates(LocalDate today) {
        return jdbc.queryForList("""
                SELECT DISTINCT l.id
                FROM loan l
                JOIN installment i ON i.loan_id = l.id
                WHERE l.status = 'ACTIVE'
                  AND i.status IN ('UNPAID', 'PARTIALLY_PAID')
                  AND i.due_date < :cutoff
                  AND NOT EXISTS (SELECT 1 FROM dispute d WHERE d.loan_id = l.id AND d.status IN ('OPEN', 'UNDER_REVIEW'))
                ORDER BY l.id
                """, Map.of("cutoff", today.minusDays(CHARGE_OFF_DAYS)), Long.class);
    }

    @Transactional
    public void chargeOff(long loanId, LocalDate today) {
        int n = jdbc.update("UPDATE loan SET status = 'CHARGED_OFF', closed_on = :d WHERE id = :id AND status = 'ACTIVE'",
                Map.of("id", loanId, "d", today));
        if (n == 0) throw new IllegalStateException("loan " + loanId + " is not ACTIVE");
    }
}
