package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.junit.RowsmithTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Reference solution for benchmark task t3. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class PayoffServiceTest {

    @Autowired PayoffService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    @Test
    void payoffSettlesOutstandingAndClosesTheLoan() {
        LocalDate payoffDate = LocalDate.now();
        Row loan = rowsmith.create("loan", l -> l.apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("amount", 100).set("paid_amount", 100).apply(Presets::paidInstallment))
                .with("installment", 1, i -> i.set("amount", 100).set("paid_amount", 40).apply(Presets::partiallyPaidInstallment))
                .with("installment", 1, i -> i.set("amount", 100)));

        long paymentId = service.payoff(loan.getLong("id"), "ACH", payoffDate);

        Map<String, Object> payment = jdbc.queryForMap("SELECT amount, status, loan_id FROM payment WHERE id = ?", paymentId);
        assertThat((BigDecimal) payment.get("amount")).isEqualByComparingTo("160.00");
        assertThat(payment.get("status")).isEqualTo("SETTLED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM installment WHERE loan_id = ? AND (status <> 'PAID' OR paid_amount <> amount)",
                Long.class, loan.getLong("id"))).isZero();
        Map<String, Object> l = jdbc.queryForMap("SELECT status::text AS status, closed_on FROM loan WHERE id = ?", loan.getLong("id"));
        assertThat(l.get("status")).isEqualTo("PAID_OFF");
        assertThat(((java.sql.Date) l.get("closed_on")).toLocalDate()).isEqualTo(payoffDate);
    }

    @Test
    void onlyActiveLoansCanBePaidOff() {
        Row loan = rowsmith.create("loan", l -> l.set("status", "PENDING").with("installment", 2));
        assertThatThrownBy(() -> service.payoff(loan.getLong("id"), "CARD", LocalDate.now())).isInstanceOf(IllegalStateException.class);
    }
}
