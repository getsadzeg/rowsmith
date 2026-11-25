package com.example.lending.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hand-written fixtures for integration tests: inserts rows with plain SQL and returns generated ids.
 * Extend it when a test needs another table.
 */
public class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final JdbcTemplate jdbc;

    public TestData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static int next() {
        return SEQ.incrementAndGet();
    }

    public long merchant() {
        int n = next();
        return jdbc.queryForObject("INSERT INTO merchant (name, legal_name, country, tax_id) VALUES (?, ?, 'US', ?) RETURNING id",
                Long.class, "Merchant " + n, "Merchant " + n + " LLC", String.format("12-%07d", n));
    }

    public long store(long merchantId) {
        return jdbc.queryForObject("INSERT INTO store (merchant_id, name, city, state, timezone) VALUES (?, ?, 'Austin', 'TX', 'America/Chicago') RETURNING id",
                Long.class, merchantId, "Store " + next());
    }

    public long customer() {
        int n = next();
        return jdbc.queryForObject("""
                INSERT INTO customer (first_name, last_name, email, date_of_birth, ssn_last4)
                VALUES ('Test', ?, ?, DATE '1990-01-01', '1234') RETURNING id
                """, Long.class, "Customer" + n, "customer" + n + "@example.com");
    }

    public long address(long customerId, String city, boolean primary) {
        return jdbc.queryForObject("""
                INSERT INTO address (customer_id, line1, city, state, postal_code, is_primary)
                VALUES (?, '1 Main St', ?, 'TX', '73301', ?) RETURNING id
                """, Long.class, customerId, city, primary);
    }

    public long kyc(long customerId, String status) {
        return jdbc.queryForObject("INSERT INTO kyc_check (customer_id, provider, status) VALUES (?, 'acme-kyc', ?) RETURNING id",
                Long.class, customerId, status);
    }

    public long loan(long customerId, long storeId, String status, BigDecimal principal, int termMonths, LocalDate originatedOn) {
        return jdbc.queryForObject("""
                INSERT INTO loan (customer_id, store_id, principal, apr, term_months, status, originated_on)
                VALUES (?, ?, ?, 19.99, ?, ?::loan_status, ?) RETURNING id
                """, Long.class, customerId, storeId, principal, termMonths, status, originatedOn);
    }

    /** An ACTIVE loan (1200.00 over 12 months, originated 3 months ago) with a fresh merchant, store and customer. */
    public long activeLoan() {
        long store = store(merchant());
        return loan(customer(), store, "ACTIVE", new BigDecimal("1200.00"), 12, LocalDate.now().minusMonths(3));
    }

    public long installment(long loanId, int seq, LocalDate dueDate, BigDecimal amount, BigDecimal paidAmount, String status) {
        return jdbc.queryForObject("""
                INSERT INTO installment (loan_id, seq, due_date, amount, paid_amount, status)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, loanId, seq, dueDate, amount, paidAmount, status);
    }

    public long unpaidInstallment(long loanId, int seq, LocalDate dueDate) {
        return installment(loanId, seq, dueDate, new BigDecimal("100.00"), BigDecimal.ZERO, "UNPAID");
    }

    public long dispute(long loanId, String status) {
        boolean closed = status.equals("RESOLVED") || status.equals("REJECTED");
        return jdbc.queryForObject("""
                INSERT INTO dispute (loan_id, reason, status, resolved_at)
                VALUES (?, 'customer disputes the charge', ?, CASE WHEN ? THEN now() END) RETURNING id
                """, Long.class, loanId, status, closed);
    }
}
