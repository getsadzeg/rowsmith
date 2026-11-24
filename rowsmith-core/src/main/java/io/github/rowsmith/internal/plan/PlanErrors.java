package io.github.rowsmith.internal.plan;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.ValueSource;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.ForeignKey;
import io.github.rowsmith.internal.schema.UniqueKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class PlanErrors {

    private final PlannedValues values;

    PlanErrors(PlannedValues values) {
        this.values = values;
    }

    static String setupValue(ColumnDef c) {
        return "setup value(" + c.qualifiedName() + ")";
    }

    static String setHint(List<String> columns) {
        return "set " + String.join(", ", columns) + " with .set(...) in the test or with RowsmithSetup.value(...)";
    }

    RowsmithException repeats(Node n, UniqueKey taken) {
        List<String> generated = n.numberedColumns(taken);
        generated.removeIf(name -> n.sources.get(n.table.column(name)) == ValueSource.SETUP);
        if (generated.isEmpty()) return uniqueViolation(n, taken);
        return new RowsmithException(ErrorCode.CANNOT_GENERATE, "every row number Rowsmith tried for " + n.label + " repeats a value under "
                + taken.describe() + "; " + setHint(generated));
    }

    RowsmithException partitionKey(Node n, List<String> keys) {
        return new RowsmithException(ErrorCode.CANNOT_GENERATE, n.table.displayName() + " is partitioned and has no DEFAULT partition; "
                + setHint(keys) + " so the row lands in a partition");
    }

    RowsmithException duplicate(Node n, UniqueKey uk) {
        List<String> generated = new ArrayList<>();
        for (ColumnDef c : uk.columns) if (n.chosenByRowsmith(c)) generated.add(c.name);
        if (generated.isEmpty()) return uniqueViolation(n, uk);
        return new RowsmithException(ErrorCode.CANNOT_GENERATE, "the generated values of " + n.label + " repeat another row under " + uk.describe()
                + "; " + setHint(generated));
    }

    private RowsmithException uniqueViolation(Node n, UniqueKey uk) {
        List<String> parts = new ArrayList<>();
        for (UniqueKey.KeyPart part : uk.parts) {
            parts.add(part.text() + " = " + switch (part) {
                case UniqueKey.ColumnPart c -> tupleDisplay(n, c.column());
                case UniqueKey.ExprPart e -> "?";
            });
        }
        String kind = uk.primary ? "primary key" : uk.isPartial() ? "partial unique index" : "unique key";
        return new RowsmithException(ErrorCode.UNIQUE_VIOLATION, n.label + " would duplicate another row on " + kind + " " + uk.describe()
                + " (" + String.join(", ", parts) + ")"
                + (uk.isPartial() ? ". Only one row per key may match the predicate; change a value so it does not match, or use a different parent" : "")
                + ". " + sourceHint(n, uk.columns));
    }

    private String tupleDisplay(Node n, ColumnDef c) {
        return switch (values.tupleValue(n, c)) {
            case Cell.Known k -> show(k.value());
            case Cell.Pending p -> p.toString();
            case Cell.Unknown() -> "?";
        };
    }

    private static String sourceHint(Node n, List<ColumnDef> cols) {
        List<String> s = new ArrayList<>();
        for (ColumnDef c : cols) {
            if (n.sources.get(c) == ValueSource.USER) s.add(c.name + " from set");
            if (n.sources.get(c) == ValueSource.SETUP) s.add(c.name + " from " + setupValue(c));
            for (ForeignKey fk : n.table.foreignKeys) {
                if (fk.columns.contains(c) && n.refs().get(fk) instanceof Ref.Existing) s.add(c.name + " from use(...)");
                if (fk.columns.contains(c) && fk == n.relFk()) s.add(c.name + " from with(...) on " + n.related.label);
            }
        }
        return s.isEmpty() ? "" : "Values came from: " + String.join(", ", new LinkedHashSet<>(s)) + ".";
    }

    private static String show(Object v) {
        if (v == null) return "NULL";
        if (v instanceof String s) return "\"" + s + "\"";
        if (v instanceof BigDecimal b) return b.toPlainString();
        return String.valueOf(v);
    }
}
