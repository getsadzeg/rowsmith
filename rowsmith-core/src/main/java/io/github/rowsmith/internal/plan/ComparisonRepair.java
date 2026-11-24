package io.github.rowsmith.internal.plan;

import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.ValueSource;
import io.github.rowsmith.internal.plan.PgEval.Truth;
import io.github.rowsmith.internal.schema.CheckConstraint;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.sql.ColumnRules;
import io.github.rowsmith.internal.sql.Op;
import io.github.rowsmith.internal.sql.SqlExpr;
import java.util.Optional;

final class ComparisonRepair {

    private final PgEval db;
    private final PlannedValues values;
    private final RowFiller filler;

    ComparisonRepair(PgEval db, PlannedValues values, RowFiller filler) {
        this.db = db;
        this.values = values;
        this.filler = filler;
    }

    boolean repair(Node n, CheckConstraint ch) {
        if (ch.expr == null) return false;
        boolean moved = false;
        for (SqlExpr conj : ColumnRules.conjuncts(ch.expr)) {
            if (!(conj instanceof SqlExpr.Bin b && b.op().isComparison() && b.left() instanceof SqlExpr.Col l && b.right() instanceof SqlExpr.Col r)) continue;
            if (values.truth(values.probe(n, conj)) != Truth.FALSE) continue;
            moved |= move(n, ch, l, b.op(), r) || move(n, ch, r, b.op().flip(), l);
        }
        return moved;
    }

    private boolean move(Node n, CheckConstraint ch, SqlExpr.Col target, Op op, SqlExpr.Col other) {
        ColumnDef c = n.table.column(target.name());
        if (c == null || n.sources.get(c) != ValueSource.ROWSMITH) return false;
        Optional<String> side = values.probe(n, other).flatMap(db::value).map(PgEval.Value::text);
        if (side.isEmpty()) return false;
        Assignment before = n.assignment(c);
        try {
            filler.regenerate(n, c, ColumnRules.bound(op, filler.typed(c, side.get()), ch.name));
            return true;
        } catch (RowsmithException noValueFits) {
            n.assign(c, before);
            return false;
        }
    }
}
