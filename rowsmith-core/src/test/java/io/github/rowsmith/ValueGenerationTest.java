package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

class ValueGenerationTest {

    @Test
    void lengthsCountCharactersAsPostgresDoes() {
        Rowsmith rs = Rowsmith.builder(Pg.withSchema("""
                CREATE TABLE mark (id serial PRIMARY KEY, sign varchar(1) NOT NULL CHECK (sign IN ('😀', 'ab')))
                """)).build();
        assertThat(rs.create("mark").values()).containsEntry("sign", "😀");
    }

    @Test
    void excludedValuesAreNotCountedAsAllowed() {
        DataSource ds = Pg.withSchema("""
                CREATE TABLE task (id serial PRIMARY KEY, prio int NOT NULL CHECK (prio BETWEEN 1 AND 3 AND prio <> 2),
                                   debt int NOT NULL CHECK (debt BETWEEN -3 AND -1 AND debt <> -2));
                CREATE TABLE slot (id serial PRIMARY KEY, prio int NOT NULL UNIQUE CHECK (prio BETWEEN 1 AND 4 AND prio NOT IN (2, 3)));
                """);
        Rowsmith rs = Rowsmith.of(ds);
        List<Row> tasks = rs.createMany("task", 7);
        assertThat(tasks).extracting(t -> t.getInt("prio")).containsOnly(1, 3);
        assertThat(tasks).extracting(t -> t.getInt("debt")).containsOnly(-1, -3);
        assertThat(rs.createMany("slot", 2)).extracting(t -> t.getInt("prio")).containsExactly(1, 4);
        assertThatThrownBy(() -> rs.create("slot"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_GENERATE))
                .hasMessageContaining("allow only 2 values");
    }

    @Test
    void posixClassesInRegexChecks() {
        DataSource ds = Pg.withSchema("""
                CREATE TABLE token (id serial PRIMARY KEY,
                  code text NOT NULL CHECK (code ~ '^[[:xdigit:]]{4}[[:space:]][[:punct:]][[:word:][:blank:]]{3}$'));
                CREATE TABLE raw (id serial PRIMARY KEY, bell text NOT NULL CHECK (bell ~ '^[[:cntrl:]]$'));
                """);
        Rowsmith rs = Rowsmith.builder(ds).build();
        rs.createMany("token", 20, t -> {});
        assertThat(Pg.count(ds, "token")).isEqualTo(20);
        assertThatThrownBy(() -> rs.create("raw"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_GENERATE))
                .hasMessageContaining("does not generate the class [:cntrl:]");
    }

    @ParameterizedTest
    @ValueSource(ints = {5, -5})
    void timestamptzBoundsHoldInTheClocksZone(int offsetHours) {
        DataSource ds = Pg.withSchema("""
                CREATE TABLE slot (id serial PRIMARY KEY,
                  starts_at timestamptz NOT NULL CHECK (starts_at >= '2030-01-01 00:00:00+00' AND starts_at <= '2030-01-01 00:00:00+00'));
                """);
        Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.ofHours(offsetHours));
        Rowsmith rs = Rowsmith.builder(ds).clock(clock).build();
        assertThat(rs.create("slot").getOffsetDateTime("starts_at").toInstant()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
    }

    @Test
    void aPartitionKeyIsSetByTheTestUnlessADefaultPartitionTakesAnyValue() {
        DataSource ds = Pg.withSchema("""
                CREATE TABLE reading (id serial, level numeric NOT NULL) PARTITION BY RANGE (level);
                CREATE TABLE reading_low PARTITION OF reading FOR VALUES FROM (0) TO (10);
                CREATE TABLE gauge (id serial, level numeric NOT NULL) PARTITION BY RANGE (level);
                CREATE TABLE gauge_low PARTITION OF gauge FOR VALUES FROM (0) TO (10);
                CREATE TABLE gauge_rest PARTITION OF gauge DEFAULT;
                """);
        Rowsmith rs = Rowsmith.builder(ds).build();
        assertThatThrownBy(() -> rs.create("reading"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_GENERATE))
                .hasMessageContaining("reading is partitioned and has no DEFAULT partition; "
                        + "set level with .set(...) in the test or with RowsmithSetup.value(...) so the row lands in a partition");
        assertThat(rs.create("reading", r -> r.set("level", 5)).getBigDecimal("level")).isEqualByComparingTo("5");
        Rowsmith withSetup = Rowsmith.builder(ds).setup(c -> c.value("reading", "level", n -> n)).build();
        assertThat(withSetup.create("reading").getBigDecimal("level")).isEqualByComparingTo("1");
        assertThat(rs.create("gauge").getBigDecimal("level")).isNotNull();
    }

    @Test
    void aUniqueTimeColumnRunsOutAfterEverySecondOfTheDay() {
        DataSource ds = Pg.withSchema("""
                CREATE TABLE slot (id serial PRIMARY KEY, at time NOT NULL UNIQUE);
                CREATE TABLE full_slot (id serial PRIMARY KEY, at time NOT NULL UNIQUE);
                INSERT INTO full_slot (at) SELECT time '00:00' + s * interval '1 second' FROM generate_series(0, 86399) s;
                """);
        Rowsmith rs = Rowsmith.builder(ds).build();
        assertThat(rs.createMany("slot", 2, s -> {})).extracting(r -> r.get("at")).containsExactly(LocalTime.of(0, 0, 1), LocalTime.of(0, 0, 2));
        assertThatThrownBy(() -> rs.create("full_slot"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_GENERATE))
                .hasMessageContaining("full_slot.at is unique but a day has only 86400 seconds");
    }

    @Test
    void aConstantPostgresCannotComputeIsLeftUnknown() {
        DataSource ds = Pg.withSchema("""
                CREATE TABLE note (id serial PRIMARY KEY,
                  tenant int NOT NULL DEFAULT current_setting('app.tenant')::int CHECK (tenant > 0 OR tenant <> current_setting('app.tenant')::int),
                  qty int NOT NULL CHECK (qty BETWEEN 3 AND 2 + 2));
                """);
        Row note = Rowsmith.builder(ds).build().create("note", n -> n.set("tenant", 7));
        assertThat(note.getInt("tenant")).isEqualTo(7);
        assertThat(note.getInt("qty")).isBetween(3, 4);
    }

    @Test
    void timestamptzTextIsReadInTheClocksZoneLikeALocalDateTime() {
        DataSource dubai = new DelegatingDataSource(Pg.withSchema("CREATE TABLE ev (id serial PRIMARY KEY, at timestamptz NOT NULL)")) {
            @Override
            public Connection getConnection() throws SQLException {
                Connection c = super.getConnection();
                try (Statement st = c.createStatement()) {
                    st.execute("SET TimeZone = 'Asia/Dubai'");
                }
                return c;
            }
        };
        Rowsmith rs = Rowsmith.builder(dubai).clock(Pg.clockAt(LocalDate.of(2026, 1, 15))).build();
        Instant tenUtc = Instant.parse("2026-01-01T10:00:00Z");
        assertThat(rs.create("ev", e -> e.set("at", "2026-01-01 10:00")).getOffsetDateTime("at").toInstant()).isEqualTo(tenUtc);
        assertThat(rs.create("ev", e -> e.set("at", LocalDateTime.of(2026, 1, 1, 10, 0))).getOffsetDateTime("at").toInstant()).isEqualTo(tenUtc);
        assertThat(rs.create("ev", e -> e.set("at", "2026-01-01 12:00+02")).getOffsetDateTime("at").toInstant()).isEqualTo(tenUtc);
    }
}
