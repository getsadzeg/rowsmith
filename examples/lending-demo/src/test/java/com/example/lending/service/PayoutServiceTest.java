package com.example.lending.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lending.Presets;
import com.example.lending.TestcontainersConfiguration;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.junit.RowsmithTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Reference solution for benchmark task t6. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RowsmithTest
class PayoutServiceTest {

    @Autowired PayoutService service;
    @Autowired JdbcTemplate jdbc;
    Rowsmith rowsmith;

    @Test
    void payoutCoversEligibleLoansOnly() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        Row store = rowsmith.create("store");
        Row active = rowsmith.create("loan", l -> l.use(store).set("status", "ACTIVE").set("principal", 1000).set("originated_on", start));
        Row paidOff = rowsmith.create("loan", l -> l.use(store).set("status", "PAID_OFF").set("principal", 500).set("originated_on", end));
        Row disputed = rowsmith.create("loan", l -> l.use(store).set("status", "ACTIVE").set("principal", 700).set("originated_on", start.plusDays(3))
                .with("dispute", 1, d -> d.apply(Presets::disputeUnderReview)));
        Row before = rowsmith.create("loan", l -> l.use(store).set("status", "ACTIVE").set("principal", 300).set("originated_on", start.minusDays(1)));
        Row pending = rowsmith.create("loan", l -> l.use(store).set("status", "PENDING").set("principal", 200).set("originated_on", start.plusDays(2)));

        long payoutId = service.createPayout(store.getLong("merchant_id"), start, end);

        List<Long> loans = jdbc.queryForList("SELECT loan_id FROM payout_item WHERE payout_id = ? ORDER BY loan_id", Long.class, payoutId);
        assertThat(loans).containsExactly(active.getLong("id"), paidOff.getLong("id"));
        assertThat(jdbc.queryForObject("SELECT amount FROM merchant_payout WHERE id = ?", BigDecimal.class, payoutId))
                .isEqualByComparingTo("1455.00");
        assertThat(loans).doesNotContain(disputed.getLong("id"), before.getLong("id"), pending.getLong("id"));
    }
}
