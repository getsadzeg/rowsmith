package io.github.rowsmith.internal.plan;

import io.github.rowsmith.internal.plan.PgEval.Probe;
import io.github.rowsmith.internal.plan.PgEval.Truth;
import io.github.rowsmith.internal.schema.CheckConstraint;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.sql.ColumnRules;
import io.github.rowsmith.internal.sql.SqlExpr;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CheckVerifier {

    record Failure(CheckConstraint check, ColumnDef domainColumn) {
        Collection<String> columns() {
            return domainColumn != null ? List.of(domainColumn.name) : check.columns;
        }
    }

    private record Asked(Node node, Failure failure) {}

    private final PgEval db;
    private final PlannedValues values;
    private final PlanErrors errors;
    private final ComparisonRepair repair;
    private final KeyRegistry keys;

    CheckVerifier(PgEval db, PlannedValues values, PlanErrors errors, ComparisonRepair repair, KeyRegistry keys) {
        this.db = db;
        this.values = values;
        this.errors = errors;
        this.repair = repair;
        this.keys = keys;
    }

    void verify(List<Node> nodes) {
        Set<Node> moved = new LinkedHashSet<>();
        for (Asked a : failing(nodes)) {
            if (a.failure().domainColumn() == null && repair.repair(a.node(), a.failure().check())) moved.add(a.node());
        }
        moved.forEach(keys::unregister);
        for (Node n : moved) {
            keys.checkKeys(n, true);
            keys.register(n, true);
        }
        List<Asked> still = failing(nodes);
        if (!still.isEmpty()) throw errors.cannotSatisfy(still.get(0).node(), still.get(0).failure());
    }

    private List<Asked> failing(List<Node> nodes) {
        Map<Asked, List<Probe>> asked = new LinkedHashMap<>();
        for (Node n : nodes) {
            for (CheckConstraint ch : n.table.checks) asked.put(new Asked(n, new Failure(ch, null)), checkProbes(n, ch));
            for (Map.Entry<ColumnDef, List<CheckConstraint>> e : n.model.domainChecks.entrySet()) {
                for (CheckConstraint ch : e.getValue()) asked.put(new Asked(n, new Failure(ch, e.getKey())), domainProbes(n, e.getKey(), ch));
            }
        }
        List<Probe> all = new ArrayList<>();
        asked.values().forEach(all::addAll);
        Map<Probe, Truth> truths = db.truths(all);
        return asked.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(p -> truths.get(p) == Truth.FALSE)).map(Map.Entry::getKey).toList();
    }

    private List<Probe> checkProbes(Node n, CheckConstraint ch) {
        if (ch.expr == null) return values.probe(n, ch.body, ch.columns).stream().toList();
        List<Probe> out = new ArrayList<>();
        for (SqlExpr conj : ColumnRules.conjuncts(ch.expr)) values.probe(n, conj).ifPresent(out::add);
        return out;
    }

    private List<Probe> domainProbes(Node n, ColumnDef c, CheckConstraint ch) {
        if (ch.expr == null) return values.domainProbe(n, c, ch.body).stream().toList();
        List<Probe> out = new ArrayList<>();
        for (SqlExpr conj : ColumnRules.conjuncts(ch.expr)) values.domainProbe(n, c, SqlExpr.sql(conj)).ifPresent(out::add);
        return out;
    }

    static Set<ColumnDef> chosenByRowsmith(Node n, Collection<String> names) {
        Set<ColumnDef> out = new LinkedHashSet<>();
        for (String name : names) {
            ColumnDef c = n.table.column(name);
            if (c != null && n.chosenByRowsmith(c)) out.add(c);
        }
        return out;
    }
}
