package io.github.rowsmith.internal.plan;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.ValueSource;
import io.github.rowsmith.internal.schema.CheckConstraint;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.ForeignKey;
import io.github.rowsmith.internal.schema.UniqueKey;
import io.github.rowsmith.internal.sql.ColumnRules;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    RowsmithException cannotSatisfy(Node n, CheckVerifier.Failure f) {
        List<String> generated = CheckVerifier.chosenByRowsmith(n, f.columns()).stream().map(c -> c.name).toList();
        if (generated.isEmpty()) return checkViolation(n, f);
        return new RowsmithException(ErrorCode.CANNOT_GENERATE, "the generated values of " + n.label + " do not satisfy " + what(f) + ": "
                + f.check().body + "; " + setHint(generated));
    }

    private RowsmithException checkViolation(Node n, CheckVerifier.Failure f) {
        CheckConstraint ch = f.check();
        List<String> cols = new ArrayList<>(f.columns());
        List<String> vals = new ArrayList<>();
        Set<String> sources = new LinkedHashSet<>();
        for (String name : cols) {
            ColumnDef c = n.table.column(name);
            if (c == null) continue;
            vals.add(name + " = " + show(values.valueOf(n, name)));
            if (n.sources.get(c) == ValueSource.SETUP) sources.add(name + " from " + setupValue(c));
        }
        StringBuilder sb = new StringBuilder();
        if (vals.size() == 1) sb.append(n.table.displayName()).append('.').append(vals.get(0));
        else sb.append(n.table.displayName()).append(" values (").append(String.join(", ", vals)).append(")");
        sb.append(vals.size() == 1 ? " violates " : " violate ").append(what(f));
        String allowed = cols.size() == 1 ? allowedText(n, n.table.column(cols.get(0))) : null;
        if (allowed != null) sb.append("; ").append(allowed);
        else sb.append(": ").append(ch.body);
        if (!sources.isEmpty()) sb.append(" [").append(String.join(", ", sources)).append("]");
        return new RowsmithException(ErrorCode.CHECK_VIOLATION, sb.toString());
    }

    private static String what(CheckVerifier.Failure f) {
        return f.domainColumn() != null ? "domain " + f.domainColumn().type.castName + " check " + f.check().name : "check " + f.check().name;
    }

    private static String allowedText(Node n, ColumnDef c) {
        if (c == null) return null;
        ColumnRules r = n.model.rulesFor(c);
        if (r.isEmpty()) return null;
        if (r.allowed != null) {
            List<String> a = new ArrayList<>();
            for (Object o : r.allowed) a.add(String.valueOf(o));
            return "allowed: " + String.join(", ", a);
        }
        List<String> parts = new ArrayList<>();
        if (r.lo != null) parts.add((r.loInclusive ? ">= " : "> ") + show(r.lo));
        if (r.hi != null) parts.add((r.hiInclusive ? "<= " : "< ") + show(r.hi));
        if (r.minLen != null) parts.add("length >= " + r.minLen);
        if (r.maxLen != null) parts.add("length <= " + r.maxLen);
        if (!r.excluded.isEmpty()) parts.add("not " + r.excluded);
        if (!r.likes.isEmpty()) parts.add("LIKE '" + r.likes.get(0) + "'");
        if (!r.regexes.isEmpty()) parts.add("~ '" + r.regexes.get(0) + "'");
        return parts.isEmpty() ? null : "must be " + String.join(" and ", parts);
    }

    private static String show(Cell v) {
        return v instanceof Cell.Known k ? show(k.value()) : "?";
    }

    private static String show(Object v) {
        if (v == null) return "NULL";
        if (v instanceof String s) return "\"" + s + "\"";
        if (v instanceof BigDecimal b) return b.toPlainString();
        return String.valueOf(v);
    }
}
