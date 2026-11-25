package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.junit.RowsmithTest;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class PaymentRetryServiceTest {

    @Autowired PaymentRetryService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    final LocalDate today = LocalDate.now();

    /** An installment due 5 days ago on an ACTIVE loan. */
    Row dueInstallment() {
        return rowsmith.create("installment", i -> i.set("due_date", today.minusDays(5)).set("status", "UNPAID")
                .set("paid_amount", 0).set("amount", 100).use(rowsmith.create("loan", l -> l.apply(Presets::activeLoan))));
    }

    Row card(Row installment, boolean isDefault) {
        Object customer = rowsmith.find("loan", installment.get("loan_id")).get("customer_id");
        return rowsmith.create("payment_method", p -> p.set("customer_id", rowsmith.find("customer", customer))
                .set("type", "CARD").set("card_brand", "VISA").set("exp_month", 12).set("exp_year", 2030).set("routing_number", null)
                .set("is_default", isDefault).set("status", "ACTIVE"));
    }

    void failedAttempt(Row installment, Row method, int attemptNo, String code) {
        rowsmith.create("payment_attempt", a -> a.use(installment).use(method).set("attempt_no", attemptNo)
                .set("status", "FAILED").set("failure_code", code).set("attempted_at", "now-1d"));
    }

    List<Map<String, Object>> scheduled(Row installment) {
        return jdbc.queryForList("SELECT * FROM payment_attempt WHERE installment_id = ? AND status = 'SCHEDULED'", installment.<Long>id());
    }

    @Test
    void softDeclineIsRetriedOnTheDefaultMethod() {
        Row installment = dueInstallment();
        Row oldCard = card(installment, false);
        Row defaultCard = card(installment, true);
        failedAttempt(installment, oldCard, 1, "INSUFFICIENT_FUNDS");

        assertThat(service.scheduleRetries(today)).isEqualTo(1);

        List<Map<String, Object>> next = scheduled(installment);
        assertThat(next).hasSize(1);
        assertThat(next.get(0).get("attempt_no")).isEqualTo(2);
        assertThat(((Date) next.get(0).get("scheduled_for")).toLocalDate()).isEqualTo(today.plusDays(3));
        assertThat(next.get(0).get("payment_method_id")).isEqualTo(defaultCard.id());
    }

    @Test
    void noRetryAfterTheThirdAttempt() {
        Row installment = dueInstallment();
        Row card = card(installment, true);
        for (int n = 1; n <= 3; n++) failedAttempt(installment, card, n, "INSUFFICIENT_FUNDS");

        service.scheduleRetries(today);

        assertThat(scheduled(installment)).isEmpty();
    }

    @Test
    void noRetryAfterAHardDecline() {
        Row installment = dueInstallment();
        failedAttempt(installment, card(installment, true), 1, "ACCOUNT_CLOSED");

        service.scheduleRetries(today);

        assertThat(scheduled(installment)).isEmpty();
    }

    @Test
    void openCollectionCaseBlocksRetryButClosedDoesNot() {
        Row open = dueInstallment();
        failedAttempt(open, card(open, true), 1, "INSUFFICIENT_FUNDS");
        rowsmith.create("collection_case", c -> c.set("loan_id", rowsmith.find("loan", open.get("loan_id"))).set("status", "OPEN"));
        Row closed = dueInstallment();
        failedAttempt(closed, card(closed, true), 1, "INSUFFICIENT_FUNDS");
        rowsmith.create("collection_case", c -> c.set("loan_id", rowsmith.find("loan", closed.get("loan_id"))).set("status", "CLOSED")
                .set("opened_on", "today-10d").set("closed_on", "today").set("close_reason", "PAID"));

        service.scheduleRetries(today);

        assertThat(scheduled(open)).isEmpty();
        assertThat(scheduled(closed)).hasSize(1);
    }
}
