package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class TakenValuesTest {

    static final String LABELS = """
            CREATE TABLE label (
              id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              name text NOT NULL,
              color text NOT NULL,
              archived boolean NOT NULL DEFAULT false,
              UNIQUE (name, color));
            CREATE UNIQUE INDEX label_lower_name ON label (lower(name));
            CREATE UNIQUE INDEX label_active_color ON label (color) WHERE NOT archived;
            """;

    @Test
    void aLongRunOfTakenValuesIsSkipped() {
        DataSource ds = Pg.withSchema(LABELS);
        Rowsmith.of(ds).createMany("label", 40);
        List<Row> more = Rowsmith.of(ds).createMany("label", 3);
        assertThat(more).extracting(r -> r.getString("name")).containsExactly("name-41", "name-42", "name-43");
    }

    @Test
    void valuesTakenByOtherRowsAreSkippedOneByOne() {
        DataSource ds = Pg.withSchema(LABELS);
        Pg.exec(ds, "INSERT INTO label (name, color) VALUES ('NAME-2', 'red'), ('x', 'color-3')");
        List<Row> labels = Rowsmith.of(ds).createMany("label", 3);
        assertThat(labels).extracting(r -> r.getString("name")).containsExactly("name-1", "name-4", "name-5");
    }

    @Test
    void aDryRunSkipsTheSameValues() {
        DataSource ds = Pg.withSchema(LABELS);
        Rowsmith.of(ds).createMany("label", 2);
        Rowsmith rs = Rowsmith.of(ds);
        Plan plan = rs.plan("label");
        assertThat(plan.rows().get(0).values().get("name")).isEqualTo("name-3");
        assertThat(rs.create("label").getString("name")).isEqualTo("name-3");
    }

    @Test
    void valuesThatRepeatAndAreAllTakenFailClearly() {
        DataSource ds = Pg.withSchema("CREATE TABLE shift (id serial PRIMARY KEY, code text NOT NULL UNIQUE)");
        Pg.exec(ds, "INSERT INTO shift (code) VALUES ('day'), ('night')");
        Rowsmith rs = Rowsmith.builder(ds).setup(c -> c.value("shift", "code", n -> n % 2 == 0 ? "day" : "night")).build();
        assertThatThrownBy(() -> rs.create("shift"))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.UNIQUE_VIOLATION))
                .hasMessageContaining("shift#1 would duplicate another row on unique key");
    }
}
