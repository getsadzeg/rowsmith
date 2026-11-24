package io.github.rowsmith;

import static io.github.rowsmith.tricky.Db.PROFILE;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.rowsmith.tricky.Db.Mood;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class GeneratedTypesTest {

    @TestFactory
    Stream<DynamicNode> everyGeneratedColumnReadsAsItsDeclaredType() throws ReflectiveOperationException {
        return Stream.of(
                schema("tracker.sql", io.github.rowsmith.tracker.Db.class),
                schema("lending.sql", io.github.rowsmith.lending.Db.class),
                schema("codegen-tricky.sql", io.github.rowsmith.tricky.Db.class));
    }

    private static DynamicNode schema(String sql, Class<?> db) throws ReflectiveOperationException {
        DataSource ds = Pg.withResource(sql);
        List<DynamicNode> tables = new ArrayList<>();
        for (Field f : db.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !(f.get(null) instanceof Table table)) continue;
            tables.add(DynamicTest.dynamicTest(table.qualifiedName(), () -> {
                Rowsmith rs = Rowsmith.builder(ds).build();
                Row row = rs.create(table);
                List<String> read = new ArrayList<>();
                for (Field cf : table.getClass().getFields()) {
                    Column<?> column = (Column<?>) cf.get(table);
                    Object value = row.get(column);
                    Object raw = row.values().get(column.name());
                    if (value != null) read.add(column.name());
                    assertThat(raw).as(column.toString()).isEqualTo(raw == null ? null : toDb(column, value));
                }
                assertThat(read).isNotEmpty();
            }));
        }
        return DynamicContainer.dynamicContainer(sql, tables);
    }

    @Test
    void arrayElementsReadLikeScalarsOfTheirType() {
        Rowsmith rs = Rowsmith.builder(Pg.withResource("codegen-tricky.sql")).build();
        Row p = rs.create(PROFILE, s -> s
                .set(PROFILE.SMALL_LIST, List.of(1, 2))
                .set(PROFILE.RATIOS, List.of(0.5))
                .set(PROFILE.DATES, List.of(LocalDate.of(2026, 1, 2)))
                .set(PROFILE.STAMPS, List.of(OffsetDateTime.parse("2026-01-02T10:00:00Z")))
                .set(PROFILE.PAST_MOODS, List.of(Mood.VERY_HAPPY, Mood._1ST))
                .set(PROFILE.QUIET_TZ, "10:00:00+02"));
        assertThat(p.get(PROFILE.SMALL_LIST)).containsExactly(1, 2);
        assertThat(p.get(PROFILE.RATIOS)).containsExactly(0.5);
        assertThat(p.get(PROFILE.DATES)).containsExactly(LocalDate.of(2026, 1, 2));
        assertThat(p.get(PROFILE.STAMPS)).singleElement().satisfies(t -> assertThat(t.toInstant()).isEqualTo(OffsetDateTime.parse("2026-01-02T10:00:00Z").toInstant()));
        assertThat(p.get(PROFILE.PAST_MOODS)).containsExactly(Mood.VERY_HAPPY, Mood._1ST);
        assertThat(p.get(PROFILE.QUIET_TZ)).isEqualTo("10:00:00+02");
    }

    @Test
    void multiDimensionalArraysReadAsNestedLists() {
        Rowsmith rs = Rowsmith.builder(Pg.withSchema("CREATE TABLE grid (id serial PRIMARY KEY, cells int[][])")).build();
        Row g = rs.create("grid", s -> s.set("cells", "{{1,2},{3,4}}"));
        assertThat(g.values().get("cells")).isEqualTo(List.of(List.of(1, 2), List.of(3, 4)));
    }

    @SuppressWarnings("unchecked")
    private static <T> Object toDb(Column<T> column, Object value) {
        return column.toDb((T) value);
    }
}
