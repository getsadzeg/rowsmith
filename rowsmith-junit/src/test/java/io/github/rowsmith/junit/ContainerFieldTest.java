package io.github.rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rowsmith.RelativeTime;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import java.sql.DriverManager;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@RowsmithTest
class ContainerFieldTest {

    static final PostgreSQLContainer PG = new PostgreSQLContainer(System.getProperty("rowsmith.test.pgImage", "postgres:17-alpine"))
            .withCopyFileToContainer(MountableFile.forClasspathResource("schema.sql"), "/docker-entrypoint-initdb.d/schema.sql");

    static {
        PG.start();
    }

    Rowsmith fieldInjected;

    @Test
    void parameterInjection(Rowsmith rowsmith) {
        Row inst = rowsmith.create("installment", i -> i.set("due_date", RelativeTime.today().minusDays(11)));
        assertThat(inst.getLocalDate("due_date")).isEqualTo(LocalDate.now().minusDays(11));
        assertThat(count("installment")).isEqualTo(1);
    }

    @Test
    void fieldInjectionSharesTheSameInstance(Rowsmith rowsmith) {
        assertThat(fieldInjected).isSameAs(rowsmith);
        fieldInjected.create("loan", l -> l.with("installment", 2));
        assertThat(count("loan")).isEqualTo(1);
    }

    @AfterAll
    static void everythingWasCleanedUp() {
        for (String t : new String[] {"merchant", "store", "customer", "loan", "installment"}) {
            assertThat(count(t)).as(t).isZero();
        }
    }

    static long count(String table) {
        try (var c = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var rs = c.createStatement().executeQuery("SELECT count(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
