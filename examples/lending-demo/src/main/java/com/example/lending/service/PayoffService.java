package com.example.lending.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Early payoff: the customer pays everything that is still owed. Creates one SETTLED payment for the
 * outstanding balance, marks every open installment PAID, and closes the loan (PAID_OFF, closed_on).
 */
@Service
public class PayoffService {

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public PayoffService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Returns the id of the payoff payment. Only ACTIVE loans can be paid off. */
    @Transactional
    public long payoff(long loanId, String method, LocalDate payoffDate) {
        String status = jdbc.queryForObject("SELECT status::text FROM loan WHERE id = :id FOR UPDATE", Map.of("id", loanId), String.class);
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("loan " + loanId + " is " + status + ", only ACTIVE loans can be paid off");
        }
        BigDecimal outstanding = jdbc.queryForObject("""
                SELECT coalesce(sum(amount - paid_amount), 0) FROM installment
                WHERE loan_id = :id AND status IN ('UNPAID', 'PARTIALLY_PAID')
                """, Map.of("id", loanId), BigDecimal.class);
        if (outstanding.signum() == 0) {
            throw new IllegalStateException("loan " + loanId + " has nothing outstanding");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Long paymentId = jdbc.queryForObject("""
                INSERT INTO payment (loan_id, amount, method, status, external_ref, paid_at)
                VALUES (:loan, :amount, :method, 'SETTLED', :ref, :now) RETURNING id
                """, new MapSqlParameterSource().addValue("loan", loanId).addValue("amount", outstanding)
                .addValue("method", method).addValue("ref", "PAYOFF-" + UUID.randomUUID().toString().replace("-", "")).addValue("now", now), Long.class);
        jdbc.update("""
                UPDATE installment SET paid_amount = amount, status = 'PAID', paid_at = :now
                WHERE loan_id = :id AND status IN ('UNPAID', 'PARTIALLY_PAID')
                """, new MapSqlParameterSource().addValue("id", loanId).addValue("now", now));
        jdbc.update("UPDATE loan SET status = 'PAID_OFF', closed_on = :date WHERE id = :id",
                new MapSqlParameterSource().addValue("id", loanId).addValue("date", payoffDate));
        return paymentId;
    }
}
