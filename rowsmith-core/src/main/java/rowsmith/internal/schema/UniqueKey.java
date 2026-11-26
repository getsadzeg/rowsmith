package rowsmith.internal.schema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import rowsmith.internal.sql.SqlExpr;

public final class UniqueKey {

    public sealed interface KeyPart {
        String text();
    }

    public record ColumnPart(ColumnDef column) implements KeyPart {
        @Override
        public String text() { return column.name; }
    }

    public record ExprPart(String text, String type, SqlExpr expr) implements KeyPart {}

    public final String name;
    public final boolean primary;
    public final List<KeyPart> parts;
    public final List<ColumnDef> columns;
    public final String predicate;
    public final SqlExpr predicateExpr;
    public final boolean nullsNotDistinct;

    public UniqueKey(String name, boolean primary, List<KeyPart> parts, String predicate, SqlExpr predicateExpr, boolean nullsNotDistinct) {
        this.name = name;
        this.primary = primary;
        this.parts = List.copyOf(parts);
        this.columns = parts.stream().<ColumnDef>mapMulti((p, out) -> { if (p instanceof ColumnPart c) out.accept(c.column()); }).toList();
        this.predicate = predicate;
        this.predicateExpr = predicateExpr;
        this.nullsNotDistinct = nullsNotDistinct;
    }

    public boolean isPartial() { return predicate != null; }

    public boolean isPlainColumns() { return columns.size() == parts.size(); }

    public Set<String> readColumns() {
        Set<String> out = new LinkedHashSet<>();
        for (KeyPart p : parts) {
            switch (p) {
                case ColumnPart c -> out.add(c.column().name);
                case ExprPart e -> { if (e.expr() != null) out.addAll(SqlExpr.columnsOf(e.expr())); }
            }
        }
        return out;
    }

    public String describe() {
        String s = name + " (" + String.join(", ", parts.stream().map(KeyPart::text).toList()) + ")";
        return predicate == null ? s : s + " WHERE " + predicate;
    }

    @Override
    public String toString() { return describe(); }
}
