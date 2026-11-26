package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rowsmith.internal.util.Savepoints;

class SavepointsTest {

    static final int ROWS = 300;
    static final String OPEN_SUBTRANSACTIONS = """
            SELECT s.subxact_count FROM pg_stat_get_backend_idset() b, pg_stat_get_backend_subxact(b) s
            WHERE pg_stat_get_backend_pid(b) = pg_backend_pid()""";

    // pg_stat_get_backend_subxact arrived in PostgreSQL 16; older servers cannot report open subtransactions.
    static final int SUBXACT_STATS_SINCE = 16;

    static DataSource ds;

    @BeforeAll
    static void db() {
        ds = Pg.withSchema("CREATE TABLE ev (id serial PRIMARY KEY, at timestamptz NOT NULL)");
    }

    @Test
    void readingTextValuesInATransactionLeavesNoSavepointsOpen() {
        Assumptions.assumeTrue(Pg.serverMajorVersion(ds) >= SUBXACT_STATS_SINCE, "needs pg_stat_get_backend_subxact (PostgreSQL 16+)");
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        Rowsmith rs = Rowsmith.of(ds);
        long subtransactions = tx.execute(status -> {
            status.setRollbackOnly();
            rs.createMany("ev", ROWS, e -> e.set("at", "2030-01-02 03:04:05"));
            return new JdbcTemplate(ds).queryForObject(OPEN_SUBTRANSACTIONS, Long.class);
        });
        assertThat(subtransactions).as("at most the create call's own savepoint").isLessThanOrEqualTo(1);
    }

    @Test
    void aRuntimeExceptionRollsBackTheWorkAndReleasesTheSavepoint() throws SQLException {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            conn.setAutoCommit(false);
            assertThatThrownBy(() -> Savepoints.isolated(conn, () -> {
                st.execute("INSERT INTO ev (at) VALUES (now())");
                throw new IllegalStateException("work failed");
            })).isInstanceOf(IllegalStateException.class);
            try (var rows = st.executeQuery("SELECT count(*) FROM ev")) {
                rows.next();
                assertThat(rows.getLong(1)).isZero();
            }
            if (conn.getMetaData().getDatabaseMajorVersion() >= SUBXACT_STATS_SINCE) {
                try (var open = st.executeQuery(OPEN_SUBTRANSACTIONS)) {
                    open.next();
                    assertThat(open.getLong(1)).isZero();
                }
            }
            conn.rollback();
        }
    }
}
