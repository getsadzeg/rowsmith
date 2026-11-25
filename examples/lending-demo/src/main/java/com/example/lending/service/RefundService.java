package com.example.lending.service;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refunds (part of) a payment. The payment becomes PARTIALLY_REFUNDED, or REFUNDED once fully refunded.
 * If the payment was applied to an installment, the installment's paid_amount goes down by the refund
 * (never below zero) and its status becomes PARTIALLY_PAID, or UNPAID when nothing remains paid.
 * Refunds beyond what remains refundable are rejected.
 */
@Service
public class RefundService {

    private final NamedParameterJdbcTemplate jdbc;

    public RefundService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long refund(long paymentId, BigDecimal amount, String reason) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("refund amount must be positive");
        Map<String, Object> p = jdbc.queryForMap("SELECT amount, status, installment_id FROM payment WHERE id = :id FOR UPDATE",
                Map.of("id", paymentId));
        String status = (String) p.get("status");
        if (!status.equals("SETTLED") && !status.equals("PARTIALLY_REFUNDED")) {
            throw new IllegalStateException("payment " + paymentId + " is " + status + " and cannot be refunded");
        }
        BigDecimal paid = (BigDecimal) p.get("amount");
        BigDecimal refunded = jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM refund WHERE payment_id = :id",
                Map.of("id", paymentId), BigDecimal.class);
        BigDecimal total = refunded.add(amount);
        if (total.compareTo(paid) > 0) {
            throw new IllegalArgumentException("refund of " + amount + " exceeds the refundable " + paid.subtract(refunded));
        }
        Long refundId = jdbc.queryForObject("INSERT INTO refund (payment_id, amount, reason) VALUES (:p, :a, :r) RETURNING id",
                new MapSqlParameterSource().addValue("p", paymentId).addValue("a", amount).addValue("r", reason), Long.class);
        jdbc.update("UPDATE payment SET status = :s WHERE id = :id",
                Map.of("s", total.compareTo(paid) == 0 ? "REFUNDED" : "PARTIALLY_REFUNDED", "id", paymentId));
        Object installmentId = p.get("installment_id");
        if (installmentId != null) {
            jdbc.update("""
                    UPDATE installment
                    SET paid_amount = greatest(paid_amount - :a, 0),
                        status = CASE WHEN greatest(paid_amount - :a, 0) = 0 THEN 'UNPAID' ELSE 'PARTIALLY_PAID' END,
                        paid_at = NULL
                    WHERE id = :id
                    """, new MapSqlParameterSource().addValue("a", amount).addValue("id", installmentId));
        }
        return refundId;
    }
}
