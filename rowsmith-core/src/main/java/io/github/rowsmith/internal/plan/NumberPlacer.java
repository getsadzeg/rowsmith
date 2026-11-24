package io.github.rowsmith.internal.plan;

import io.github.rowsmith.internal.plan.PgEval.Input;
import io.github.rowsmith.internal.schema.UniqueKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class NumberPlacer {

    private record Attempt(int row, long number, Map<UniqueKey, List<Object>> tuples, Map<UniqueKey, List<Input>> inputs) {}

    private record KeyValue(UniqueKey key, List<Object> tuple) {}

    private final List<Node> rows;
    private final PgEval db;
    private final PlannedValues values;
    private final KeyRegistry keys;
    private final RowFiller filler;
    private final PlanErrors errors;
    private final Map<UniqueKey, Set<List<Object>>> taken = new HashMap<>();
    private final Set<KeyValue> passed = new HashSet<>();
    private final Set<Node> placing;

    NumberPlacer(List<Node> rows, PgEval db, PlannedValues values, KeyRegistry keys, RowFiller filler, PlanErrors errors) {
        this.rows = rows;
        this.db = db;
        this.values = values;
        this.keys = keys;
        this.filler = filler;
        this.errors = errors;
        this.placing = new HashSet<>(rows);
    }

    boolean place() {
        long[] start = rows.stream().mapToLong(n -> n.number).toArray();
        long[] placed = new long[rows.size()];
        long next = start[0];
        int window = 1;
        int i = 0;
        while (i < rows.size()) {
            List<Attempt> attempts = new ArrayList<>();
            for (int j = i; j < rows.size(); j++) {
                for (long y = next + j - i; y < next + j - i + window; y++) attempts.add(attempt(rows.get(j), j, y));
            }
            lookUp(attempts);
            int first = i;
            long cursor = next;
            for (Attempt a : attempts) {
                if (a.row() != i || a.number() < cursor) continue;
                UniqueKey key = takenKey(a);
                if (key != null) {
                    passed.add(new KeyValue(key, a.tuples().get(key)));
                    continue;
                }
                placed[i++] = a.number();
                a.tuples().forEach((uk, tuple) -> taken.computeIfAbsent(uk, k -> new HashSet<>()).add(tuple));
                cursor = a.number() + 1;
            }
            next = i == first ? next + window : cursor;
            window *= 2;
            if (i == first && next - start[0] - i > passed.size()) throw errors.repeats(rows.get(i), takenKey(attempts.get(0)));
        }
        boolean moved = false;
        for (int j = 0; j < rows.size(); j++) {
            filler.keepNumber(rows.get(j), placed[j]);
            moved |= placed[j] != start[j];
        }
        return moved;
    }

    private Attempt attempt(Node n, int row, long number) {
        if (n.number != number) filler.renumber(n, number);
        Map<UniqueKey, List<Object>> tuples = new LinkedHashMap<>();
        Map<UniqueKey, List<Input>> inputs = new LinkedHashMap<>();
        for (UniqueKey uk : n.model.keysWithoutFk) {
            if (n.numberedColumns(uk).isEmpty()) continue;
            Optional<List<Object>> tuple = keys.keyTuple(n, uk);
            if (tuple.isEmpty()) continue;
            tuples.put(uk, tuple.get());
            values.keyInputs(n, uk).ifPresent(in -> inputs.put(uk, in));
        }
        return new Attempt(row, number, tuples, inputs);
    }

    private void lookUp(List<Attempt> attempts) {
        Map<UniqueKey, Map<List<Object>, List<Input>>> asked = new LinkedHashMap<>();
        for (Attempt a : attempts) {
            a.inputs().forEach((uk, in) -> {
                List<Object> tuple = a.tuples().get(uk);
                if (!taken.getOrDefault(uk, Set.of()).contains(tuple)) asked.computeIfAbsent(uk, k -> new LinkedHashMap<>()).putIfAbsent(tuple, in);
            });
        }
        for (Map.Entry<UniqueKey, Map<List<Object>, List<Input>>> e : asked.entrySet()) {
            List<List<Object>> tuples = new ArrayList<>(e.getValue().keySet());
            for (int k : db.existing(rows.get(0).table, e.getKey(), new ArrayList<>(e.getValue().values()))) {
                taken.computeIfAbsent(e.getKey(), x -> new HashSet<>()).add(tuples.get(k));
            }
        }
    }

    private UniqueKey takenKey(Attempt a) {
        for (Map.Entry<UniqueKey, List<Object>> e : a.tuples().entrySet()) {
            if (taken.getOrDefault(e.getKey(), Set.of()).contains(e.getValue())) return e.getKey();
            Ref owner = keys.owner(e.getKey(), e.getValue());
            if (owner != null && !(owner instanceof Ref.Planned(Node m) && placing.contains(m))) return e.getKey();
        }
        return null;
    }
}
