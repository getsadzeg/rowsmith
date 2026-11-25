package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.junit.RowsmithTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Reference solution for benchmark task t4. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class OverdueReportServiceTest {

    @Autowired OverdueReportService service;
    Rowsmith rowsmith;

    @Test
    void countsEachOverdueLoanOncePerMerchant() {
        LocalDate today = LocalDate.now();
        Row store = rowsmith.create("store");
        // two overdue installments on one loan: counted once
        rowsmith.create("loan", l -> l.use(store).apply(Presets::activeLoan)
                .with("installment", 2, i -> i.set("amount", 50).set("due_date", today.minusDays(5))));
        // a second overdue loan
        rowsmith.create("loan", l -> l.use(store).apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("amount", 30).set("due_date", today.minusDays(1))));
        // not overdue: due today, paid, or loan not active
        rowsmith.create("loan", l -> l.use(store).apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("amount", 70).set("due_date", today)));
        rowsmith.create("loan", l -> l.use(store).apply(Presets::activeLoan)
                .with("installment", 1, i -> i.set("amount", 70).set("paid_amount", 70).apply(Presets::paidInstallment).set("due_date", today.minusDays(9))));
        rowsmith.create("loan", l -> l.use(store).apply(Presets::paidOffLoan)
                .with("installment", 1, i -> i.set("amount", 70).set("due_date", today.minusDays(9))));

        List<OverdueReportService.MerchantOverdue> report = service.report(today);

        OverdueReportService.MerchantOverdue row = report.stream()
                .filter(r -> r.merchantId() == store.getLong("merchant_id")).findFirst().orElseThrow();
        assertThat(row.overdueLoans()).isEqualTo(2);
        assertThat(row.overdueAmount()).isEqualByComparingTo("130.00");
    }
}
