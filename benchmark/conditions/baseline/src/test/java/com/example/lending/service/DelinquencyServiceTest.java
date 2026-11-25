package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.lending.TestcontainersConfiguration;
import com.example.lending.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class DelinquencyServiceTest {

    @Autowired DelinquencyService service;
    @Autowired JdbcTemplate jdbc;
    TestData data;

    final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc);
    }

    @Test
    void loanMoreThan90DaysPastDueIsACandidate() {
        long loan = data.activeLoan();
        data.unpaidInstallment(loan, 1, today.minusDays(91));
        long recent = data.activeLoan();
        data.unpaidInstallment(recent, 1, today.minusDays(30));

        assertThat(service.chargeOffCandidates(today)).contains(loan).doesNotContain(recent);
    }

    @Test
    void activeDisputeProtectsTheLoan() {
        long loan = data.activeLoan();
        data.unpaidInstallment(loan, 1, today.minusDays(120));
        data.dispute(loan, "OPEN");

        assertThat(service.chargeOffCandidates(today)).doesNotContain(loan);
    }

    @Test
    void chargeOffClosesOnlyActiveLoans() {
        long active = data.activeLoan();
        long store = data.store(data.merchant());
        long paidOff = data.loan(data.customer(), store, "PAID_OFF", new BigDecimal("500.00"), 6, today.minusYears(1));

        service.chargeOff(active, today);

        assertThat(jdbc.queryForObject("SELECT status::text FROM loan WHERE id = ?", String.class, active)).isEqualTo("CHARGED_OFF");
        assertThatThrownBy(() -> service.chargeOff(paidOff, today)).isInstanceOf(IllegalStateException.class);
    }
}
