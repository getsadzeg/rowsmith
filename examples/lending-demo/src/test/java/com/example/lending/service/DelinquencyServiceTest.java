package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import rowsmith.Row;
import rowsmith.Rowsmith;
import rowsmith.junit.RowsmithTest;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class DelinquencyServiceTest {

    @Autowired DelinquencyService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    final LocalDate today = LocalDate.now();

    @Test
    void loanMoreThan90DaysPastDueIsACandidate() {
        Row loan = rowsmith.create("loan", l -> l.apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("due_date", today.minusDays(91))));
        Row recent = rowsmith.create("loan", l -> l.apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("due_date", today.minusDays(30))));

        assertThat(service.chargeOffCandidates(today)).contains(loan.getLong("id")).doesNotContain(recent.getLong("id"));
    }

    @Test
    void activeDisputeProtectsTheLoan() {
        Row loan = rowsmith.create("loan", l -> l.apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("due_date", today.minusDays(120)))
                .with("dispute", 1, d -> d.apply(Presets::openDispute)));

        assertThat(service.chargeOffCandidates(today)).doesNotContain(loan.getLong("id"));
    }

    @Test
    void chargeOffClosesOnlyActiveLoans() {
        Row active = rowsmith.create("loan", l -> l.apply(Presets::activeLoan));
        Row paidOff = rowsmith.create("loan", l -> l.apply(Presets::paidOffLoan));

        service.chargeOff(active.getLong("id"), today);

        assertThat(jdbc.queryForObject("SELECT status::text FROM loan WHERE id = ?", String.class, active.getLong("id"))).isEqualTo("CHARGED_OFF");
        assertThatThrownBy(() -> service.chargeOff(paidOff.getLong("id"), today)).isInstanceOf(IllegalStateException.class);
    }
}
