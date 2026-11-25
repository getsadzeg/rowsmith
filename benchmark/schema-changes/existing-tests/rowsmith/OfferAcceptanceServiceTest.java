package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.lending.TestcontainersConfiguration;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.junit.RowsmithTest;
import java.math.BigDecimal;
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
class OfferAcceptanceServiceTest {

    @Autowired OfferAcceptanceService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    final LocalDate today = LocalDate.now();

    Row approvedDecision(int limit) {
        return rowsmith.create("credit_decision", d -> d.set("outcome", "APPROVED").set("approved_limit", limit));
    }

    Row offer(Row decision, int amount, int term, String expiresAt) {
        return rowsmith.create("offer", o -> o.use(decision).set("amount", amount).set("term_months", term)
                .set("apr", 9.99).set("status", "OFFERED").set("expires_at", expiresAt));
    }

    String offerStatus(Row offer) {
        return jdbc.queryForObject("SELECT status FROM offer WHERE id = ?", String.class, offer.<Long>id());
    }

    @Test
    void acceptingAnOfferCreatesTheLoanAndWithdrawsTheOthers() {
        Row decision = approvedDecision(2000);
        Row application = rowsmith.find("credit_application", decision.get("application_id"));
        Row chosen = offer(decision, 1000, 3, "now+7d");
        Row other = offer(decision, 1000, 6, "now+7d");

        long loan = service.accept(chosen.getLong("id"), today);

        Map<String, Object> l = jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", loan);
        assertThat(l.get("customer_id")).isEqualTo(application.get("customer_id"));
        assertThat(l.get("store_id")).isEqualTo(application.get("store_id"));
        assertThat((BigDecimal) l.get("principal")).isEqualByComparingTo("1000.00");
        assertThat((BigDecimal) l.get("apr")).isEqualByComparingTo("9.99");
        assertThat(l.get("term_months")).isEqualTo(3);
        assertThat(l.get("status").toString()).isEqualTo("ACTIVE");
        assertThat(((Date) l.get("originated_on")).toLocalDate()).isEqualTo(today);
        assertThat(l.get("offer_id")).isEqualTo(chosen.id());
        assertThat(offerStatus(chosen)).isEqualTo("ACCEPTED");
        assertThat(offerStatus(other)).isEqualTo("WITHDRAWN");
    }

    @Test
    void installmentsAreMonthlyAndAddUpToThePrincipal() {
        Row chosen = offer(approvedDecision(2000), 1000, 3, "now+7d");

        long loan = service.accept(chosen.getLong("id"), today);

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT due_date, amount FROM installment WHERE loan_id = ? ORDER BY seq", loan);
        assertThat(rows).hasSize(3);
        assertThat(((Date) rows.get(0).get("due_date")).toLocalDate()).isEqualTo(today.plusMonths(1));
        assertThat(((Date) rows.get(2).get("due_date")).toLocalDate()).isEqualTo(today.plusMonths(3));
        BigDecimal sum = rows.stream().map(r -> (BigDecimal) r.get("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1000.00");
    }

    @Test
    void expiredOfferIsRejected() {
        Row expired = offer(approvedDecision(2000), 1000, 3, "now-2d");

        assertThatThrownBy(() -> service.accept(expired.getLong("id"), today)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void offerAboveTheApprovedLimitIsRejected() {
        Row tooBig = offer(approvedDecision(500), 1000, 3, "now+7d");

        assertThatThrownBy(() -> service.accept(tooBig.getLong("id"), today)).isInstanceOf(IllegalStateException.class);
    }
}
