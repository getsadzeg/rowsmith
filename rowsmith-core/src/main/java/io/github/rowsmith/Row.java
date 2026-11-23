package io.github.rowsmith;

import io.github.rowsmith.internal.schema.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** A row Rowsmith inserted (values as returned by {@code INSERT ... RETURNING *}), with links to its parents and children. */
public final class Row {

    private final String table;
    private final String qualifiedTable;
    private final Map<String, Object> values;
    private final List<String> primaryKey;
    private final Schema schema;
    private final Map<String, Row> parents;
    private final Map<String, List<Row>> children;

    Row(String table, String qualifiedTable, Map<String, Object> values, List<String> primaryKey, Schema schema,
            Map<String, Row> parents, Map<String, List<Row>> children) {
        this.table = table;
        this.qualifiedTable = qualifiedTable;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.primaryKey = primaryKey;
        this.schema = schema;
        this.parents = parents;
        this.children = children;
    }

    public String table() { return table; }
    public String qualifiedTable() { return qualifiedTable; }
    public Map<String, Object> values() { return values; }

    /**
     * The primary key value (single-column PKs), as returned by the database: Long for bigint, Integer for int, UUID, ...
     * For a typed value use {@link #get(Column)} or a converting getter such as {@link #getLong(String)}.
     */
    public Object id() {
        if (primaryKey.size() != 1) {
            throw new RowsmithException(ErrorCode.NO_SINGLE_PRIMARY_KEY, table + (primaryKey.isEmpty() ? " has no primary key" : " has a composite primary key ("
                    + String.join(", ", primaryKey) + ")") + "; use get(\"column\") instead of id()");
        }
        return values.get(primaryKey.get(0));
    }

    /** The value of a column as returned by the database; see the typed getters for converted values. */
    public Object get(String column) {
        if (!values.containsKey(column)) {
            throw new RowsmithException(ErrorCode.UNKNOWN_COLUMN, "row of " + table + " has no column '" + column + "'. Columns: " + String.join(", ", values.keySet()));
        }
        return values.get(column);
    }

    /** The value of a typed column, e.g. {@code task.get(TASK.STATUS)}. */
    public <T> T get(Column<T> column) {
        requireOwnColumn(column);
        return column.fromDb(get(column.name()));
    }

    private void requireOwnColumn(Column<?> column) {
        if (!column.table().qualifiedName().equals(qualifiedTable)) {
            throw new RowsmithException(ErrorCode.UNKNOWN_COLUMN, column + " is not a column of " + qualifiedTable);
        }
    }

    public long getLong(String column) { return ((Number) require(column)).longValue(); }
    public int getInt(String column) { return ((Number) require(column)).intValue(); }
    public String getString(String column) { Object v = get(column); return v == null ? null : v.toString(); }
    public Boolean getBoolean(String column) { return as(column, Boolean.class); }
    public BigDecimal getBigDecimal(String column) {
        Object v = get(column);
        return v == null ? null : v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }
    public LocalDate getLocalDate(String column) { return as(column, LocalDate.class); }
    public LocalDateTime getLocalDateTime(String column) { return as(column, LocalDateTime.class); }
    public OffsetDateTime getOffsetDateTime(String column) { return as(column, OffsetDateTime.class); }
    public UUID getUuid(String column) { return as(column, UUID.class); }

    private <T> T as(String column, Class<T> type) {
        return type.cast(get(column));
    }

    private Object require(String column) {
        return Objects.requireNonNull(get(column), () -> table + "." + column + " is NULL");
    }

    /** The row this row references through the FK to {@code tableOrFkColumn} (a table name or an FK column name). */
    public Row parent(String tableOrFkColumn) {
        Row r = parents.get(tableOrFkColumn);
        if (r != null) return r;
        for (Row p : parents.values()) if (p.isOf(tableOrFkColumn)) return p;
        throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, "row of " + table + " has no known parent '" + tableOrFkColumn
                + "' (known: " + parentNames() + "). Only parents Rowsmith created or was given with use(...) are linked.");
    }

    /** The row this row references through a single-column foreign key, e.g. {@code task.parent(TASK.PROJECT_ID)}. */
    public Row parent(Column<?> fkColumn) {
        requireOwnColumn(fkColumn);
        return parent(fkColumn.name());
    }

    /** The row this row references in {@code table} (when only one FK points there). */
    public Row parent(Table table) {
        return parent(table.qualifiedName());
    }

    private List<String> parentNames() {
        List<String> names = new ArrayList<>();
        parents.forEach((k, v) -> names.add(v.table + " via " + k));
        return names;
    }

    /** Children created with {@code with(childTable, ...)} in the same call. */
    public List<Row> children(String childTable) {
        var t = schema.findTable(childTable);
        return t == null ? List.of() : childrenIn(t.qualifiedName());
    }

    /** Children of that table created with {@code with(child, ...)} in the same call. */
    public List<Row> children(Table child) {
        return childrenIn(child.qualifiedName());
    }

    private List<Row> childrenIn(String qualifiedName) {
        return Collections.unmodifiableList(children.getOrDefault(qualifiedName, List.of()));
    }

    private boolean isOf(String tableName) {
        return table.equals(tableName) || qualifiedTable.equals(tableName);
    }

    /** The only child of that table; fails if there are zero or several. */
    public Row child(String childTable) {
        return only(children(childTable), childTable);
    }

    /** The only child of that table; fails if there are zero or several. */
    public Row child(Table child) {
        return only(children(child), child.qualifiedName());
    }

    private Row only(List<Row> rows, String childTable) {
        if (rows.size() != 1) {
            throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, "row of " + table + " has " + rows.size() + " '" + childTable
                    + "' children; use children(...)" + (children.isEmpty() ? "" : ". Known children: " + children.keySet()));
        }
        return rows.get(0);
    }

    @Override
    public String toString() {
        return table + values;
    }
}
