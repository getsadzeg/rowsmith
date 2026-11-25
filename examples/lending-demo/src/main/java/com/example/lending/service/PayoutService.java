package com.example.lending.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merchant payouts. A payout for a merchant and period [start, end] (inclusive) covers ACTIVE and
 * PAID_OFF loans originated in the period at any of the merchant's stores, except loans with an active
 * dispute (OPEN or UNDER_REVIEW). The merchant receives 97% of each principal (3% discount rate).
 */
@Service
public class PayoutService {

    static final BigDecimal MERCHANT_SHARE = new BigDecimal("0.97");

    private final NamedParameterJdbcTemplate jdbc;

    public PayoutService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Creates the payout and its items; returns the payout id. One payout per merchant and period start. */
    @Transactional
    public long createPayout(long merchantId, LocalDate start, LocalDate end) {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM merchant_payout WHERE merchant_id = :m AND period_start = :s",
                Map.of("m", merchantId, "s", start), Integer.class);
        if (existing != null && existing > 0) {
            throw new IllegalStateException("payout for merchant " + merchantId + " starting " + start + " already exists");
        }
        List<Map<String, Object>> loans = jdbc.queryForList("""
                SELECT l.id, l.principal
                FROM loan l
                JOIN store s ON s.id = l.store_id
                WHERE s.merchant_id = :m
                  AND l.originated_on BETWEEN :start AND :end
                  AND l.status IN ('ACTIVE', 'PAID_OFF')
                  AND NOT EXISTS (SELECT 1 FROM dispute d WHERE d.loan_id = l.id AND d.status IN ('OPEN', 'UNDER_REVIEW'))
                ORDER BY l.id
                """, Map.of("m", merchantId, "start", start, "end", end));
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> l : loans) total = total.add(share((BigDecimal) l.get("principal")));
        Long payoutId = jdbc.queryForObject("""
                INSERT INTO merchant_payout (merchant_id, period_start, period_end, amount)
                VALUES (:m, :s, :e, :a) RETURNING id
                """, new MapSqlParameterSource().addValue("m", merchantId).addValue("s", start).addValue("e", end).addValue("a", total), Long.class);
        for (Map<String, Object> l : loans) {
            jdbc.update("INSERT INTO payout_item (payout_id, loan_id, amount) VALUES (:p, :l, :a)",
                    Map.of("p", payoutId, "l", l.get("id"), "a", share((BigDecimal) l.get("principal"))));
        }
        return payoutId;
    }

    static BigDecimal share(BigDecimal principal) {
        return principal.multiply(MERCHANT_SHARE).setScale(2, RoundingMode.HALF_UP);
    }
}
