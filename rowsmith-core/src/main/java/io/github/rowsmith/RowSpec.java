package io.github.rowsmith;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Describes one row to create: the values that matter for the test, children, and existing rows to
 * reuse. Everything not described is generated from the schema.
 *
 * <p>Reusable presets are plain methods taking a RowSpec, applied with {@link #apply(Consumer)} or
 * passed directly as the spec:
 * <pre>{@code
 * static void overdue(RowSpec t) { t.set(TASK.DUE_DATE, today().minusDays(3)); }
 *
 * rowsmith.create(TASK, TaskData::overdue);
 * rowsmith.create(TASK, t -> t.apply(TaskData::overdue).set(TASK.STATUS, TaskStatus.TODO));
 * }</pre>
 */
public final class RowSpec {

    /** A child relationship declared with {@code with(...)}. */
    public record Child(String table, String via, int count, Consumer<RowSpec> spec) {}

    private final String table;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final List<Child> children = new ArrayList<>();
    private final List<Row> uses = new ArrayList<>();
    private final Map<String, Row> usesByColumn = new LinkedHashMap<>();
    private final List<Column<?>> typedColumns = new ArrayList<>();

    RowSpec(String table) {
        this.table = table;
    }

    /** Sets a column to a value of its Java type. */
    public <T> RowSpec set(Column<T> column, T value) {
        typedColumns.add(column);
        values.put(column.name(), column.toDb(value));
        return this;
    }

    /** Points a foreign key column at an existing (or just created) row. */
    public RowSpec set(Column<?> fkColumn, Row row) {
        typedColumns.add(fkColumn);
        values.put(fkColumn.name(), row);
        return this;
    }

    /** Sets a date or timestamp column relative to Rowsmith's clock, e.g. {@code today().minusDays(3)}. */
    public RowSpec set(Column<? extends Temporal> column, RelativeTime when) {
        typedColumns.add(column);
        values.put(column.name(), when);
        return this;
    }

    /** Sets a nullable column to NULL. */
    public RowSpec setNull(Column<?> column) {
        typedColumns.add(column);
        values.put(column.name(), null);
        return this;
    }

    /** Applies a preset: any method taking a RowSpec, e.g. {@code apply(TaskData::overdue)}. Later calls win. */
    public RowSpec apply(Consumer<? super RowSpec> preset) {
        preset.accept(this);
        return this;
    }

    /** Creates {@code count} rows of {@code child} pointing at this row (via its only FK to this table). */
    public RowSpec with(Table child, int count) {
        return with(child.qualifiedName(), null, count, c -> {});
    }

    public RowSpec with(Table child, Consumer<RowSpec> spec) {
        return with(child.qualifiedName(), null, 1, spec);
    }

    public RowSpec with(Table child, int count, Consumer<RowSpec> spec) {
        return with(child.qualifiedName(), null, count, spec);
    }

    /** Like {@link #with(Table, int, Consumer)} but names the FK column when the child has several FKs to this table. */
    public RowSpec with(Table child, Column<?> via, int count, Consumer<RowSpec> spec) {
        if (!via.table().qualifiedName().equals(child.qualifiedName())) {
            throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, "with(" + child + ", " + via + ", ...): " + via
                    + " is not a column of " + child);
        }
        return with(child.qualifiedName(), via.name(), count, spec);
    }

    /** Reuses an existing row for one specific FK column. */
    public RowSpec use(Column<?> fkColumn, Row row) {
        typedColumns.add(fkColumn);
        usesByColumn.put(fkColumn.name(), row);
        return this;
    }

    /** Sets a column. Values: String, Number, Boolean, java.time types, UUID, List (arrays), JSON text for json/jsonb, a {@link Row} for an FK column, a {@link RelativeTime} or relative dates like "today-11d". */
    public RowSpec set(String column, Object value) {
        values.put(column, value);
        return this;
    }

    public RowSpec set(Map<String, ?> values) {
        this.values.putAll(values);
        return this;
    }

    /** Creates {@code count} rows of {@code childTable} pointing at this row (via its only FK to this table). */
    public RowSpec with(String childTable, int count) {
        return with(childTable, null, count, c -> {});
    }

    public RowSpec with(String childTable, Consumer<RowSpec> spec) {
        return with(childTable, null, 1, spec);
    }

    public RowSpec with(String childTable, int count, Consumer<RowSpec> spec) {
        return with(childTable, null, count, spec);
    }

    /** Like {@link #with(String, int, Consumer)} but names the FK column when the child has several FKs to this table. */
    public RowSpec with(String childTable, String viaColumn, int count, Consumer<RowSpec> spec) {
        if (count < 0) throw new IllegalArgumentException("with(\"" + childTable + "\", " + count + "): count must be >= 0");
        children.add(new Child(childTable, viaColumn, count, spec == null ? c -> {} : spec));
        return this;
    }

    /** Reuses an existing row for every required FK that points at that row's table. */
    public RowSpec use(Row row) {
        uses.add(row);
        return this;
    }

    /** Reuses an existing row for one specific FK column. */
    public RowSpec use(String fkColumn, Row row) {
        usesByColumn.put(fkColumn, row);
        return this;
    }

    public String table() { return table; }
    public Map<String, Object> values() { return Collections.unmodifiableMap(values); }
    public List<Child> children() { return Collections.unmodifiableList(children); }
    public List<Row> uses() { return Collections.unmodifiableList(uses); }
    public Map<String, Row> usesByColumn() { return Collections.unmodifiableMap(usesByColumn); }

    List<Column<?>> typedColumns() { return Collections.unmodifiableList(typedColumns); }

    static RowSpec of(String table, Consumer<RowSpec> spec) {
        RowSpec s = new RowSpec(table);
        if (spec != null) spec.accept(s);
        return s;
    }
}
