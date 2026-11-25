package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.lending.TestcontainersConfiguration;
import com.example.lending.support.TestData;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class OfferAcceptanceServiceTest {

    @Autowired OfferAcceptanceService service;
    @Autowired JdbcTemplate jdbc;
    TestData data;

    final LocalDate today = LocalDate.now();
    long customer;
    long store;

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc);
        customer = data.customer();
        store = data.store(data.merchant());
    }

    long approvedDecision(String limit) {
        long application = jdbc.queryForObject("""
                INSERT INTO credit_application (customer_id, store_id, requested_amount, channel, status)
                VALUES (?, ?, ?, 'ONLINE', 'DECIDED') RETURNING id
                """, Long.class, customer, store, new BigDecimal(limit));
        return jdbc.queryForObject("""
                INSERT INTO credit_decision (application_id, outcome, score, approved_limit, model_version)
                VALUES (?, 'APPROVED', 720, ?, 'v1.0.0') RETURNING id
                """, Long.class, application, new BigDecimal(limit));
    }

    long offer(long decision, String amount, int term, OffsetDateTime expiresAt) {
        return jdbc.queryForObject("""
                INSERT INTO offer (decision_id, amount, term_months, apr, expires_at)
                VALUES (?, ?, ?, 9.99, ?) RETURNING id
                """, Long.class, decision, new BigDecimal(amount), term, expiresAt);
    }

    String offerStatus(long offer) {
        return jdbc.queryForObject("SELECT status FROM offer WHERE id = ?", String.class, offer);
    }

    @Test
    void acceptingAnOfferCreatesTheLoanAndWithdrawsTheOthers() {
        long decision = approvedDecision("2000.00");
        OffsetDateTime later = OffsetDateTime.now().plusDays(7);
        long chosen = offer(decision, "1000.00", 3, later);
        long other = offer(decision, "1000.00", 6, later);

        long loan = service.accept(chosen, today);

        Map<String, Object> l = jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", loan);
        assertThat(l.get("customer_id")).isEqualTo(customer);
        assertThat(l.get("store_id")).isEqualTo(store);
        assertThat((BigDecimal) l.get("principal")).isEqualByComparingTo("1000.00");
        assertThat((BigDecimal) l.get("apr")).isEqualByComparingTo("9.99");
        assertThat(l.get("term_months")).isEqualTo(3);
        assertThat(l.get("status").toString()).isEqualTo("ACTIVE");
        assertThat(((Date) l.get("originated_on")).toLocalDate()).isEqualTo(today);
        assertThat(l.get("offer_id")).isEqualTo(chosen);
        assertThat(offerStatus(chosen)).isEqualTo("ACCEPTED");
        assertThat(offerStatus(other)).isEqualTo("WITHDRAWN");
    }

    @Test
    void installmentsAreMonthlyAndAddUpToThePrincipal() {
        long decision = approvedDecision("2000.00");
        long chosen = offer(decision, "1000.00", 3, OffsetDateTime.now().plusDays(7));

        long loan = service.accept(chosen, today);

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT due_date, amount FROM installment WHERE loan_id = ? ORDER BY seq", loan);
        assertThat(rows).hasSize(3);
        assertThat(((Date) rows.get(0).get("due_date")).toLocalDate()).isEqualTo(today.plusMonths(1));
        assertThat(((Date) rows.get(2).get("due_date")).toLocalDate()).isEqualTo(today.plusMonths(3));
        BigDecimal sum = rows.stream().map(r -> (BigDecimal) r.get("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1000.00");
    }

    @Test
    void expiredOfferIsRejected() {
        long decision = approvedDecision("2000.00");
        long expired = offer(decision, "1000.00", 3, OffsetDateTime.now().minusDays(2));

        assertThatThrownBy(() -> service.accept(expired, today)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void offerAboveTheApprovedLimitIsRejected() {
        long decision = approvedDecision("500.00");
        long tooBig = offer(decision, "1000.00", 3, OffsetDateTime.now().plusDays(7));

        assertThatThrownBy(() -> service.accept(tooBig, today)).isInstanceOf(IllegalStateException.class);
    }
}
