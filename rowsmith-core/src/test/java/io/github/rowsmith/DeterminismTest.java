package io.github.rowsmith;

import static io.github.rowsmith.lending.Db.CUSTOMER;
import static io.github.rowsmith.lending.Db.CUSTOMER_KYC;
import static io.github.rowsmith.lending.Db.DISPUTE;
import static io.github.rowsmith.lending.Db.INSTALLMENT;
import static io.github.rowsmith.lending.Db.LOAN;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DeterminismTest {

    static DataSource ds;
    static DataSource empty;

    @BeforeAll
    static void schema() {
        ds = Pg.withResource("lending.sql");
        empty = Pg.withResource("lending.sql");
    }

    private String planText() {
        Rowsmith rs = Rowsmith.builder(empty).clock(Pg.clockAt(LocalDate.of(2026, 1, 15))).build();
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\n", rs.plan(LOAN, l -> l.with(INSTALLMENT, 3).with(DISPUTE, 1)).sql())).append('\n');
        sb.append(String.join("\n", rs.planMany(CUSTOMER, 3, c -> c.with(CUSTOMER_KYC, 1)).sql())).append('\n');
        return sb.toString();
    }

    @Test
    void sameInputSameOutput() {
        assertThat(planText()).isEqualTo(planText());
    }

    @Test
    void dryRunThenCreateProducesTheSameValues() {
        Rowsmith rs = Rowsmith.builder(ds).clock(Pg.clockAt(LocalDate.of(2026, 1, 15))).build();
        Plan plan = rs.plan(CUSTOMER);
        Row c = rs.create(CUSTOMER);
        assertThat(plan.rows().get(0).values().get("email")).isEqualTo(c.get(CUSTOMER.EMAIL));
    }

    @Test
    void matchesSnapshot() throws IOException {
        String actual = planText();
        Path snapshot = Path.of("src/test/resources/snapshots/lending-plan.sql");
        if (Boolean.getBoolean("rowsmith.updateSnapshots") || !Files.exists(snapshot)) {
            Files.createDirectories(snapshot.getParent());
            Files.writeString(snapshot, actual);
        }
        assertThat(actual)
                .as("plan output changed; if intended, rerun with -Drowsmith.updateSnapshots=true")
                .isEqualTo(Files.readString(snapshot));
    }
}
