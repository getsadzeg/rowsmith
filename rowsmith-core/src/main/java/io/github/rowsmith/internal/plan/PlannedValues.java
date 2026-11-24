package io.github.rowsmith.internal.plan;

import io.github.rowsmith.internal.gen.Literals;
import io.github.rowsmith.internal.plan.PgEval.Input;
import io.github.rowsmith.internal.plan.PgEval.Probe;
import io.github.rowsmith.internal.plan.PgEval.Truth;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.UniqueKey;
import io.github.rowsmith.internal.sql.ColumnRules;
import io.github.rowsmith.internal.sql.SqlExpr;
import io.github.rowsmith.internal.sql.SqlValues;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

final class PlannedValues {

    private final PgEval db;

    PlannedValues(PgEval db) {
        this.db = db;
    }

    Cell valueOf(Node n, String colName) {
        ColumnDef c = n.table.column(colName);
        if (c == null) return new Cell.Unknown();
        if (!(n.assignment(c) instanceof Assignment.Default)) return evalForm(n.cell(c));
        return c.isGenerated() || (n.model.fkColumns.contains(c) && !n.resolved) ? new Cell.Unknown() : defaultValue(n, c);
    }

    private static Cell defaultValue(Node n, ColumnDef c) {
        if (c.isIdentity()) return new Cell.Unknown();
        if (c.defaultExpr == null) return new Cell.Known(null);
        if (c.defaultParsed == null) return new Cell.Unknown();
        return n.model.constants.value(c.defaultParsed).<Cell>map(Cell.Known::new).orElseGet(Cell.Unknown::new);
    }

    private static Cell evalForm(Cell cell) {
        if (!(cell instanceof Cell.Known(Object v))) return cell;
        if (v instanceof Integer || v instanceof Long || v instanceof Short) return new Cell.Known(BigDecimal.valueOf(((Number) v).longValue()));
        if (v instanceof UUID u) return new Cell.Known(u.toString());
        return cell;
    }

    Cell tupleValue(Node n, ColumnDef c) {
        Cell v = valueOf(n, c.name);
        return v instanceof Cell.Known k ? new Cell.Known(norm(k.value())) : v;
    }

    private static Object norm(Object v) {
        if (v instanceof Number num && !(v instanceof BigDecimal)) v = SqlValues.num(num);
        if (v instanceof BigDecimal b) return b.signum() == 0 ? "0" : b.stripTrailingZeros().toPlainString();
        if (v instanceof UUID u) return u.toString();
        if (v instanceof byte[] bytes) return HexFormat.of().formatHex(bytes);
        if (v instanceof OffsetDateTime o) return o.toInstant();
        return v;
    }

    private Optional<Input> input(Node n, ColumnDef c) {
        String baseType = Executor.castFor(c.baseType(), c);
        return n.assignment(c) instanceof Assignment.Default ? defaultInput(n, c, baseType) : planned(n.cell(c), baseType);
    }

    private static Optional<Input> defaultInput(Node n, ColumnDef c, String type) {
        if (n.model.fkColumns.contains(c) && !n.resolved) return Optional.empty();
        if (c.isGenerated() || c.isIdentity()) return Optional.empty();
        if (c.defaultExpr == null) return Optional.of(new Input.Value(null, type));
        return c.stableDefault ? Optional.of(new Input.Default(c.defaultExpr, type)) : Optional.empty();
    }

    private static Optional<Input> planned(Cell v, String type) {
        return v instanceof Cell.Known(Object k) ? Optional.of(new Input.Value(Literals.toText(k), type)) : Optional.empty();
    }

    Optional<Probe> probe(Node n, String sql, Collection<String> columns) {
        SortedMap<String, Input> inputs = new TreeMap<>();
        for (String name : columns) {
            ColumnDef c = n.table.column(name);
            Optional<Input> in = c == null ? Optional.empty() : input(n, c);
            if (in.isEmpty()) return Optional.empty();
            inputs.put(name, in.get());
        }
        return Optional.of(new Probe(inputs, sql));
    }

    Optional<Probe> probe(Node n, SqlExpr e) {
        return probe(n, SqlExpr.sql(e), SqlExpr.columnsOf(e));
    }

    Optional<Probe> exprProbe(Node n, UniqueKey.ExprPart e) {
        return e.expr() == null ? Optional.empty() : probe(n, e.text(), SqlExpr.columnsOf(e.expr()));
    }

    Optional<Probe> domainProbe(Node n, ColumnDef c, String sql) {
        return input(n, c).map(in -> new Probe(new TreeMap<>(Map.of(ColumnRules.DOMAIN_VALUE, in)), sql));
    }

    Truth truth(Optional<Probe> probe) {
        return probe.map(db::truth).orElse(Truth.UNKNOWN);
    }

    Optional<List<Input>> keyInputs(Node n, UniqueKey uk) {
        List<Input> out = new ArrayList<>();
        for (UniqueKey.KeyPart part : uk.parts) {
            Optional<Input> in = switch (part) {
                case UniqueKey.ColumnPart c -> input(n, c.column());
                case UniqueKey.ExprPart e -> exprProbe(n, e).flatMap(db::value).map(v -> new Input.Value(v.text(), e.type()));
            };
            if (in.isEmpty()) return Optional.empty();
            out.add(in.get());
        }
        return Optional.of(out);
    }
}
