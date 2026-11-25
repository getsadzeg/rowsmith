package com.example.lending.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The customer accepts one of the offers made for an approved credit decision. Creates an ACTIVE loan
 * for the application's customer and store with the offer's amount, APR and term, plus its installment
 * schedule; marks the offer ACCEPTED and withdraws the decision's other open offers.
 */
@Service
public class OfferAcceptanceService {

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public OfferAcceptanceService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Returns the id of the new loan. Throws IllegalStateException when the offer is not OFFERED, has
     * expired (expires_at before {@code today}), its decision is not APPROVED, or its amount exceeds the
     * decision's approved_limit.
     */
    @Transactional
    public long accept(long offerId, LocalDate today) {
        Map<String, Object> o = jdbc.queryForMap("""
                SELECT o.decision_id, o.amount, o.term_months, o.apr, o.status, o.expires_at::date AS expires_on,
                       d.outcome, d.approved_limit, a.customer_id, a.store_id
                FROM offer o
                JOIN credit_decision d ON d.id = o.decision_id
                JOIN credit_application a ON a.id = d.application_id
                WHERE o.id = :id
                FOR UPDATE OF o
                """, Map.of("id", offerId));
        if (!"OFFERED".equals(o.get("status"))) {
            throw new IllegalStateException("offer " + offerId + " is " + o.get("status"));
        }
        if (((java.sql.Date) o.get("expires_on")).toLocalDate().isBefore(today)) {
            throw new IllegalStateException("offer " + offerId + " has expired");
        }
        if (!"APPROVED".equals(o.get("outcome"))) {
            throw new IllegalStateException("decision for offer " + offerId + " is " + o.get("outcome"));
        }
        BigDecimal principal = (BigDecimal) o.get("amount");
        if (principal.compareTo((BigDecimal) o.get("approved_limit")) > 0) {
            throw new IllegalStateException("offer " + offerId + " exceeds the approved limit");
        }
        long decisionId = ((Number) o.get("decision_id")).longValue();
        int term = ((Number) o.get("term_months")).intValue();

        jdbc.update("UPDATE offer SET status = 'ACCEPTED', accepted_at = :now WHERE id = :id",
                new MapSqlParameterSource().addValue("id", offerId).addValue("now", OffsetDateTime.now(clock)));
        jdbc.update("UPDATE offer SET status = 'WITHDRAWN' WHERE decision_id = :d AND id <> :id AND status = 'OFFERED'",
                Map.of("d", decisionId, "id", offerId));

        Long loanId = jdbc.queryForObject("""
                INSERT INTO loan (customer_id, store_id, principal, apr, term_months, status, originated_on, offer_id)
                VALUES (:customer, :store, :principal, :apr, :term, 'ACTIVE', :today, :offer) RETURNING id
                """, new MapSqlParameterSource()
                .addValue("customer", o.get("customer_id")).addValue("store", o.get("store_id"))
                .addValue("principal", principal).addValue("apr", o.get("apr")).addValue("term", term)
                .addValue("today", today).addValue("offer", offerId), Long.class);

        BigDecimal regular = principal.divide(BigDecimal.valueOf(term), 2, RoundingMode.DOWN);
        BigDecimal last = principal.subtract(regular.multiply(BigDecimal.valueOf(term - 1)));
        for (int seq = 1; seq <= term; seq++) {
            jdbc.update("""
                    INSERT INTO installment (loan_id, seq, due_date, amount)
                    VALUES (:loan, :seq, :due, :amount)
                    """, new MapSqlParameterSource().addValue("loan", loanId).addValue("seq", seq)
                    .addValue("due", today.plusMonths(seq)).addValue("amount", seq == term ? last : regular));
        }
        return loanId;
    }
}
