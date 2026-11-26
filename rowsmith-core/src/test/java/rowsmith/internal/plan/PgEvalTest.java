package rowsmith.internal.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rowsmith.ErrorCode;
import rowsmith.Pg;
import rowsmith.RowsmithException;
import rowsmith.internal.plan.PgEval.Input;
import rowsmith.internal.plan.PgEval.Probe;
import rowsmith.internal.plan.PgEval.Truth;
import rowsmith.internal.schema.CatalogReader;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.sql.SqlExpr;
import rowsmith.internal.sql.SqlParser;

class PgEvalTest {

    static DataSource ds;

    @BeforeAll
    static void db() {
        ds = Pg.withSchema("""
                CREATE TYPE task_status AS ENUM ('TODO', 'DONE');
                CREATE TABLE task (id serial PRIMARY KEY, code int NOT NULL UNIQUE, due timestamptz);
                """);
    }

    private static Probe probe(String check, Map<String, Input> inputs) {
        return new Probe(new TreeMap<>(inputs), SqlExpr.sql(SqlParser.parse(check)));
    }

    private static Input text(String v) {
        return new Input.Value(v, "text");
    }

    private static Truth truth(String check, Map<String, Input> inputs) throws Exception {
        try (Connection c = ds.getConnection()) {
            return new PgEval(c, ZoneOffset.UTC).truth(probe(check, inputs));
        }
    }

    @Test
    void threeValuedLogicAsInACheck() throws Exception {
        String check = "(((done_at IS NULL) OR (status = 'DONE'::text)))";
        assertThat(truth(check, Map.of("done_at", new Input.Value(null, "date"), "status", text("TODO")))).isEqualTo(Truth.TRUE);
        assertThat(truth(check, Map.of("done_at", new Input.Value("2026-01-04", "date"), "status", text("TODO")))).isEqualTo(Truth.FALSE);
        assertThat(truth("((estimate > 0))", Map.of("estimate", new Input.Value(null, "int4")))).isEqualTo(Truth.NULL);
    }

    @Test
    void printedExpressionsMeanWhatPostgresWrote() throws Exception {
        assertThat(truth("((num_nonnulls(a, b) = 1))", Map.of("a", text("x"), "b", text(null)))).isEqualTo(Truth.TRUE);
        assertThat(truth("((upper((code)::text) = (code)::text))", Map.of("code", text("abc")))).isEqualTo(Truth.FALSE);
        assertThat(truth("((sku ~~ 'SKU-%'::text))", Map.of("sku", text("SKU-1")))).isEqualTo(Truth.TRUE);
        assertThat(truth("((sku !~~* 'sku-%'::text))", Map.of("sku", text("SKU-1")))).isEqualTo(Truth.FALSE);
        assertThat(truth("(CASE operation WHEN 'INS'::text THEN (old_id IS NULL) ELSE false END)",
                Map.of("operation", text("UPD"), "old_id", text(null)))).isEqualTo(Truth.FALSE);
        assertThat(truth("((VALUE)[1] < (VALUE)[2])", Map.of("value", new Input.Value("{1,2}", "int4[]")))).isEqualTo(Truth.TRUE);
        assertThat(truth("(EXTRACT(day FROM period) = (1)::numeric)", Map.of("period", new Input.Value("2026-03-01", "date")))).isEqualTo(Truth.TRUE);
        assertThat(truth("(\"substring\"((VALUE)::text, 2, 1) = 'o'::text)", Map.of("value", text("Todo")))).isEqualTo(Truth.TRUE);
        assertThat(truth("((status = ANY (ARRAY['TODO'::task_status, 'DONE'::task_status])))",
                Map.of("status", new Input.Value("DONE", "task_status")))).isEqualTo(Truth.TRUE);
        assertThat(truth("((due_on > created_on))", Map.of("due_on", new Input.Value("2026-01-04", "date"),
                "created_on", new Input.Default("CURRENT_DATE", "date")))).isNotEqualTo(Truth.UNKNOWN);
    }

    @Test
    void domainCheckTextReadsTheValueAsVALUE() throws Exception {
        try (Connection c = ds.getConnection()) {
            Probe raw = new Probe(new TreeMap<>(Map.of("value", new Input.Value("3", "int4"))), "((VALUE > 0) AND (VALUE < 3))");
            assertThat(new PgEval(c, ZoneOffset.UTC).truth(raw)).isEqualTo(Truth.FALSE);
        }
    }

    @Test
    void batchesBeyondTheBindParameterLimitAreSplit() throws Exception {
        List<Probe> probes = new ArrayList<>();
        for (int i = 1; i <= PgEval.MAX_PARAMETERS + 1; i++) probes.add(probe("((estimate > 0))", Map.of("estimate", new Input.Value(String.valueOf(i), "int4"))));
        try (Connection c = ds.getConnection()) {
            assertThat(new PgEval(c, ZoneOffset.UTC).truths(probes).values()).containsOnly(Truth.TRUE);
        }
    }

    @Test
    void anErrorIsNotKnownAndDoesNotAbortTheTransaction() throws Exception {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            PgEval db = new PgEval(c, ZoneOffset.UTC);
            Probe bad = probe("(((title)::integer > 0))", Map.of("title", text("task-1")));
            Probe good = probe("((length(title) > 0))", Map.of("title", text("task-1")));
            assertThat(db.truth(bad)).isEqualTo(Truth.UNKNOWN);
            assertThat(db.truths(List.of(good))).containsEntry(good, Truth.TRUE);
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
                assertThat(rs.next()).isTrue();
            }
            c.rollback();
        }
    }

    @Test
    void anErrorLeavesOnlyItsOwnProbeUnknown() throws Exception {
        Probe bad = probe("(((title)::integer > 0))", Map.of("title", text("task-1")));
        Probe sameShape = probe("(((title)::integer > 0))", Map.of("title", text("7")));
        Probe other = probe("((length(title) > 0))", Map.of("title", text("task-1")));
        try (Connection c = ds.getConnection()) {
            assertThat(new PgEval(c, ZoneOffset.UTC).truths(List.of(bad, sameShape, other)))
                    .containsEntry(bad, Truth.UNKNOWN).containsEntry(sameShape, Truth.TRUE).containsEntry(other, Truth.TRUE);
        }
    }

    @Test
    void aFailedLookUpOfExistingKeysNamesTheKey() throws Exception {
        try (Connection c = ds.getConnection()) {
            TableDef task = CatalogReader.read(c).table("task");
            UniqueKey code = task.uniqueKeys.stream().filter(uk -> !uk.primary).findFirst().orElseThrow();
            assertThatThrownBy(() -> new PgEval(c, ZoneOffset.UTC).existing(task, code, List.of(List.of(new Input.Value("seven", "int4")))))
                    .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.DATABASE_ERROR))
                    .hasMessageContaining("task_code_key").hasMessageContaining("seven");
        }
    }

    @Test
    void textIsReadInTheGivenZoneWithoutChangingTheSessionZone() throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("SET TimeZone = 'Asia/Dubai'");
            ColumnDef due = CatalogReader.read(c).table("task").column("due");
            PgEval db = new PgEval(c, ZoneId.of("America/New_York"));
            assertThat(((OffsetDateTime) db.read("2026-01-01 10:00", due)).toInstant()).isEqualTo(Instant.parse("2026-01-01T15:00:00Z"));
            c.setAutoCommit(false);
            assertThat(((OffsetDateTime) db.read("2026-07-01 10:00", due)).toInstant()).isEqualTo(Instant.parse("2026-07-01T14:00:00Z"));
            assertThat(((OffsetDateTime) new PgEval(c, ZoneOffset.ofHours(-3)).read("2026-01-01 10:00", due)).toInstant())
                    .isEqualTo(Instant.parse("2026-01-01T13:00:00Z"));
            try (ResultSet rs = st.executeQuery("SHOW TimeZone")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("Asia/Dubai");
            }
            c.rollback();
        }
    }
}
