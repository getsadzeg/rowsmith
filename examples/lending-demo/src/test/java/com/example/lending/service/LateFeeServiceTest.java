package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

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

/** Reference solution for benchmark tasks t1 (disputes) and t2 (one fee per installment). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class LateFeeServiceTest {

    @Autowired LateFeeService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    final LocalDate today = LocalDate.now();

    long fees(Row installment) {
        return jdbc.queryForObject("SELECT count(*) FROM late_fee WHERE installment_id = ?", Long.class, installment.getLong("id"));
    }

    Row overdueLoan(String disputeStatus) {
        return rowsmith.create("loan", l -> {
            l.apply(Presets::activeLoan).with("installment", 1, i -> i.apply(Presets::overdueInstallment));
            if (disputeStatus != null) l.with("dispute", 1, d -> d.set("status", disputeStatus));
        });
    }

    @Test
    void overdueInstallmentOnActiveLoanGetsAFee() {
        Row loan = overdueLoan(null);
        service.assessLateFees(today);
        assertThat(fees(loan.child("installment"))).isEqualTo(1);
    }

    @Test
    void activeDisputesBlockTheFee() {
        Row open = overdueLoan("OPEN");
        Row review = overdueLoan("UNDER_REVIEW");
        service.assessLateFees(today);
        assertThat(fees(open.child("installment"))).isZero();
        assertThat(fees(review.child("installment"))).isZero();
    }

    @Test
    void resolvedDisputeDoesNotBlockTheFee() {
        Row loan = rowsmith.create("loan", l -> l.apply(Presets::activeLoan)
                .with("installment", 1, i -> i.apply(Presets::overdueInstallment))
                .with("dispute", 1, d -> d.apply(Presets::resolvedDispute)));
        service.assessLateFees(today);
        assertThat(fees(loan.child("installment"))).isEqualTo(1);
    }

    @Test
    void withinGracePeriodNoFee() {
        Row loan = rowsmith.create("loan", l -> l.apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("due_date", today.minusDays(LateFeeService.GRACE_DAYS))));
        service.assessLateFees(today);
        assertThat(fees(loan.child("installment"))).isZero();
    }

    @Test
    void assessesAtMostOneFeePerInstallment() {
        Row loan = rowsmith.create("loan", l -> l.apply(Presets::activeLoan).with("installment", 2, i -> i.apply(Presets::overdueInstallment)));
        service.assessLateFees(today);
        for (Row i : loan.children("installment")) assertThat(fees(i)).isEqualTo(1);
        service.assessLateFees(today);
        service.assessLateFees(today.plusDays(30));
        for (Row i : loan.children("installment")) assertThat(fees(i)).isEqualTo(1);
    }
}
