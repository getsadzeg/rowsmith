package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lending.TestcontainersConfiguration;
import com.example.lending.support.TestData;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PaymentRetryServiceTest {

    @Autowired PaymentRetryService service;
    @Autowired JdbcTemplate jdbc;
    TestData data;

    final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc);
    }

    /** An installment due 5 days ago on a new ACTIVE loan; returns {loan, installment, customer}. */
    long[] dueInstallment() {
        long customer = data.customer();
        long loan = data.loan(customer, data.store(data.merchant()), "ACTIVE", new BigDecimal("1200.00"), 12, today.minusMonths(1));
        long installment = data.unpaidInstallment(loan, 1, today.minusDays(5));
        return new long[] {loan, installment, customer};
    }

    long card(long customer, String last4, boolean isDefault) {
        return jdbc.queryForObject("""
                INSERT INTO payment_method (customer_id, type, fingerprint, last4, card_brand, exp_month, exp_year, is_default, holder_name)
                VALUES (?, 'CARD', ?, ?, 'VISA', 12, 2030, ?, 'Test Holder') RETURNING id
                """, Long.class, customer, "fp-" + customer + "-" + last4, last4, isDefault);
    }

    void failedAttempt(long installment, long method, int attemptNo, String code) {
        jdbc.update("""
                INSERT INTO payment_attempt (installment_id, payment_method_id, attempt_no, amount, status, scheduled_for, attempted_at, failure_code)
                VALUES (?, ?, ?, 100.00, 'FAILED', ?, now(), ?)
                """, installment, method, attemptNo, today.minusDays(1), code);
    }

    long collectionCase(long loan, String status) {
        long agent = jdbc.queryForObject("""
                INSERT INTO collection_agent (email, full_name, team) VALUES (?, 'Agent', 'EARLY') RETURNING id
                """, Long.class, "agent" + loan + status + "@example.com");
        boolean closed = status.equals("CLOSED");
        return jdbc.queryForObject("""
                INSERT INTO collection_case (loan_id, agent_id, stage, status, opened_on, closed_on, close_reason)
                VALUES (?, ?, 'EARLY', ?, ?, ?, ?) RETURNING id
                """, Long.class, loan, agent, status, today.minusDays(20), closed ? today.minusDays(2) : null, closed ? "PAID" : null);
    }

    List<Map<String, Object>> scheduled(long installment) {
        return jdbc.queryForList("SELECT * FROM payment_attempt WHERE installment_id = ? AND status = 'SCHEDULED'", installment);
    }

    @Test
    void softDeclineIsRetriedOnTheDefaultMethod() {
        long[] d = dueInstallment();
        long oldCard = card(d[2], "1111", false);
        long defaultCard = card(d[2], "2222", true);
        failedAttempt(d[1], oldCard, 1, "INSUFFICIENT_FUNDS");

        assertThat(service.scheduleRetries(today)).isEqualTo(1);

        List<Map<String, Object>> next = scheduled(d[1]);
        assertThat(next).hasSize(1);
        assertThat(next.get(0).get("attempt_no")).isEqualTo(2);
        assertThat(((Date) next.get(0).get("scheduled_for")).toLocalDate()).isEqualTo(today.plusDays(3));
        assertThat(next.get(0).get("payment_method_id")).isEqualTo(defaultCard);
    }

    @Test
    void noRetryAfterTheThirdAttempt() {
        long[] d = dueInstallment();
        long card = card(d[2], "3333", true);
        failedAttempt(d[1], card, 1, "INSUFFICIENT_FUNDS");
        failedAttempt(d[1], card, 2, "INSUFFICIENT_FUNDS");
        failedAttempt(d[1], card, 3, "INSUFFICIENT_FUNDS");

        service.scheduleRetries(today);

        assertThat(scheduled(d[1])).isEmpty();
    }

    @Test
    void noRetryAfterAHardDecline() {
        long[] d = dueInstallment();
        long card = card(d[2], "4444", true);
        failedAttempt(d[1], card, 1, "ACCOUNT_CLOSED");

        service.scheduleRetries(today);

        assertThat(scheduled(d[1])).isEmpty();
    }

    @Test
    void openCollectionCaseBlocksRetryButClosedDoesNot() {
        long[] open = dueInstallment();
        failedAttempt(open[1], card(open[2], "5555", true), 1, "INSUFFICIENT_FUNDS");
        collectionCase(open[0], "OPEN");
        long[] closed = dueInstallment();
        failedAttempt(closed[1], card(closed[2], "6666", true), 1, "INSUFFICIENT_FUNDS");
        collectionCase(closed[0], "CLOSED");

        service.scheduleRetries(today);

        assertThat(scheduled(open[1])).isEmpty();
        assertThat(scheduled(closed[1])).hasSize(1);
    }
}
