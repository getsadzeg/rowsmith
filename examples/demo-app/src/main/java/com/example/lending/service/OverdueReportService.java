package com.example.lending.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-merchant overdue report. A loan is overdue when it is ACTIVE and has at least one UNPAID or
 * PARTIALLY_PAID installment due before {@code today}. Each overdue loan counts once per merchant;
 * the amount is the outstanding sum of the overdue installments. Merchants without overdue loans are omitted.
 */
@Service
public class OverdueReportService {

    public record MerchantOverdue(long merchantId, String merchantName, long overdueLoans, BigDecimal overdueAmount) {}

    private final NamedParameterJdbcTemplate jdbc;

    public OverdueReportService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<MerchantOverdue> report(LocalDate today) {
        return jdbc.query("""
                SELECT m.id, m.name, count(DISTINCT l.id) AS loans, sum(i.amount - i.paid_amount) AS amount
                FROM installment i
                JOIN loan l ON l.id = i.loan_id
                JOIN store s ON s.id = l.store_id
                JOIN merchant m ON m.id = s.merchant_id
                WHERE l.status = 'ACTIVE'
                  AND i.status IN ('UNPAID', 'PARTIALLY_PAID')
                  AND i.due_date < :today
                GROUP BY m.id, m.name
                ORDER BY amount DESC, m.id
                """, Map.of("today", today),
                (rs, n) -> new MerchantOverdue(rs.getLong("id"), rs.getString("name"), rs.getLong("loans"), rs.getBigDecimal("amount")));
    }
}
