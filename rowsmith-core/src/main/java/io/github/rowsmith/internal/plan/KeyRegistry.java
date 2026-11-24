package io.github.rowsmith.internal.plan;

import io.github.rowsmith.internal.plan.PgEval.Probe;
import io.github.rowsmith.internal.plan.PgEval.Truth;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.UniqueKey;
import io.github.rowsmith.internal.sql.SqlExpr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class KeyRegistry {

    private final GenState state;
    private final PlannedValues values;
    private final PgEval db;
    private final PlanErrors errors;

    KeyRegistry(GenState state, PlannedValues values, PgEval db, PlanErrors errors) {
        this.state = state;
        this.values = values;
        this.db = db;
        this.errors = errors;
    }

    Optional<List<Object>> keyTuple(Node n, UniqueKey uk) {
        if (uk.predicateExpr != null) {
            Truth p = values.truth(values.probe(n, uk.predicate, SqlExpr.columnsOf(uk.predicateExpr)));
            if (p == Truth.FALSE || p == Truth.NULL) return Optional.empty();
        }
        Map<UniqueKey.ExprPart, Probe> exprParts = new HashMap<>();
        for (UniqueKey.KeyPart part : uk.parts) {
            if (!(part instanceof UniqueKey.ExprPart e)) continue;
            Optional<Probe> probe = values.exprProbe(n, e);
            if (probe.isEmpty()) return Optional.empty();
            exprParts.put(e, probe.get());
        }
        Map<Probe, Optional<PgEval.Value>> computed = db.values(exprParts.values());
        List<Object> tuple = new ArrayList<>();
        for (UniqueKey.KeyPart part : uk.parts) {
            Object v;
            switch (part) {
                case UniqueKey.ColumnPart c -> {
                    switch (values.tupleValue(n, c.column())) {
                        case Cell.Known k -> v = k.value();
                        case Cell.Pending p -> v = p;
                        case Cell.Unknown() -> { return Optional.empty(); }
                    }
                }
                case UniqueKey.ExprPart e -> {
                    Optional<PgEval.Value> computedPart = computed.get(exprParts.get(e));
                    if (computedPart.isEmpty()) return Optional.empty();
                    v = computedPart.get().text();
                }
            }
            if (v == null && !uk.nullsNotDistinct) return Optional.empty();
            tuple.add(v);
        }
        return Optional.of(tuple);
    }

    private static boolean keyHasFk(Node n, UniqueKey uk) {
        for (ColumnDef c : uk.columns) if (n.model.fkColumns.contains(c)) return true;
        return false;
    }

    List<UniqueKey> duplicateKeys(Node n, boolean includeFk) {
        List<UniqueKey> out = new ArrayList<>();
        for (UniqueKey uk : n.table.uniqueKeys) {
            if (!includeFk && keyHasFk(n, uk)) continue;
            if (keyTuple(n, uk).filter(t -> heldByAnother(owner(uk, t), n)).isPresent()) out.add(uk);
        }
        return out;
    }

    void checkKeys(Node n, boolean includeFkKeys) {
        List<UniqueKey> dups = duplicateKeys(n, includeFkKeys);
        if (!dups.isEmpty()) throw errors.duplicate(n, dups.get(0));
    }

    Ref owner(UniqueKey uk, List<Object> tuple) {
        return state.unique.getOrDefault(uk, Map.of()).get(tuple);
    }

    static boolean heldByAnother(Ref owner, Node n) {
        return owner != null && !owner.equals(new Ref.Planned(n));
    }

    void unregister(Node n) {
        Ref self = new Ref.Planned(n);
        for (Map<List<Object>, Ref> owners : state.unique.values()) owners.values().removeIf(self::equals);
    }

    void register(Node n, boolean includeFk) {
        for (UniqueKey uk : n.table.uniqueKeys) {
            if (!includeFk && keyHasFk(n, uk)) continue;
            keyTuple(n, uk).ifPresent(t -> state.unique.computeIfAbsent(uk, k -> new HashMap<>()).put(t, new Ref.Planned(n)));
        }
    }

    int held(UniqueKey uk) {
        return state.unique.getOrDefault(uk, Map.of()).size();
    }

    UniqueKey takenKey(Node n) {
        for (UniqueKey uk : n.model.keysWithoutFk) {
            if (n.numberedColumns(uk).isEmpty()) continue;
            if (keyTuple(n, uk).filter(t -> heldByAnother(owner(uk, t), n)).isPresent()) return uk;
        }
        return null;
    }

    long takenTuples(Node n) {
        long count = 0;
        for (UniqueKey uk : n.model.keysWithoutFk) {
            if (!n.numberedColumns(uk).isEmpty()) count += held(uk);
        }
        return count;
    }

    Map<UniqueKey, List<Object>> tuples(Node n) {
        Map<UniqueKey, List<Object>> out = new HashMap<>();
        for (UniqueKey uk : n.table.uniqueKeys) keyTuple(n, uk).ifPresent(t -> out.put(uk, t));
        return out;
    }

    void inserted(Node n, Map<UniqueKey, List<Object>> planned) {
        for (UniqueKey uk : n.table.uniqueKeys) {
            Map<List<Object>, Ref> seen = state.unique.get(uk);
            if (seen != null && planned.containsKey(uk)) seen.remove(planned.get(uk));
            keyTuple(n, uk).ifPresent(t -> state.unique.computeIfAbsent(uk, k -> new HashMap<>()).put(t, new Ref.Existing(n.row)));
        }
    }
}
