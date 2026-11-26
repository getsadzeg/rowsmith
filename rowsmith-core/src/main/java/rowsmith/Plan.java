package rowsmith;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The result of a dry run: the rows Rowsmith would insert, in insert order, and the SQL.
 * Values not known before inserting (identity keys, defaults) are shown as {@code <table#n.column>} or {@code DEFAULT}.
 */
public final class Plan {

    /**
     * One planned row. {@code label} is e.g. "task#1"; {@code sources} says where each column's value comes from;
     * {@code reason} says why the row exists (requested, child, auto parent).
     */
    public record PlannedRow(String label, String table, Map<String, Object> values, Map<String, ValueSource> sources, String reason) {}

    private final List<PlannedRow> rows;
    private final List<String> sql;

    Plan(List<PlannedRow> rows, List<String> sql) {
        this.rows = List.copyOf(rows);
        this.sql = List.copyOf(sql);
    }

    public List<PlannedRow> rows() { return rows; }

    public List<String> sql() { return Collections.unmodifiableList(sql); }

    /** Number of rows per table, in insert order. */
    public Map<String, Long> counts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (PlannedRow r : rows) m.merge(r.table(), 1L, Long::sum);
        return m;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            PlannedRow r = rows.get(i);
            String sources = r.sources().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("; "));
            sb.append(r.label()).append(" (").append(r.reason()).append(")\n  ").append(sql.get(i)).append("\n  ").append(sources).append('\n');
        }
        return sb.toString();
    }
}
