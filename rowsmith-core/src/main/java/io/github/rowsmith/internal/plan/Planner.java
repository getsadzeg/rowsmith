package io.github.rowsmith.internal.plan;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowSpec;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.internal.schema.ForeignKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

final class Planner {

    final KeyRegistry keys;
    private final PgEval db;
    private final PlannedValues values;
    private final PlanErrors errors;
    private final RowFiller filler;
    private final Expander expander;
    List<Node> nodes = List.of();

    Planner(Engine engine, GenState state, PgEval db) {
        this.db = db;
        this.values = new PlannedValues(db);
        this.errors = new PlanErrors(values);
        this.keys = new KeyRegistry(state, values, db, errors);
        this.filler = new RowFiller(engine, db, state, keys, errors);
        this.expander = new Expander(engine, db, filler, keys);
    }

    List<Node> plan(List<RowSpec> roots) {
        nodes = expander.expand(roots);
        return topo();
    }

    private List<Node> topo() {
        Map<Node, Set<Node>> deps = new HashMap<>();
        Map<Node, List<Node>> dependents = new HashMap<>();
        for (Node n : nodes) {
            Set<Node> d = new LinkedHashSet<>();
            for (Map.Entry<ForeignKey, Ref> e : n.refs().entrySet()) {
                if (e.getValue() instanceof Ref.Planned(Node m) && m != n && !n.deferred.contains(e.getKey())) d.add(m);
            }
            deps.put(n, d);
            for (Node m : d) dependents.computeIfAbsent(m, k -> new ArrayList<>()).add(n);
        }
        PriorityQueue<Node> ready = new PriorityQueue<>((a, b) -> Integer.compare(a.id, b.id));
        Map<Node, Integer> remaining = new HashMap<>();
        for (Node n : nodes) {
            remaining.put(n, deps.get(n).size());
            if (deps.get(n).isEmpty()) ready.add(n);
        }
        List<Node> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            Node n = ready.poll();
            order.add(n);
            for (Node m : dependents.getOrDefault(n, List.of())) {
                if (remaining.merge(m, -1, Integer::sum) == 0) ready.add(m);
            }
        }
        if (order.size() != nodes.size()) {
            List<String> stuck = new ArrayList<>();
            for (Node n : nodes) if (!order.contains(n)) stuck.add(n.label);
            throw new RowsmithException(ErrorCode.REQUIRED_FK_CYCLE, "rows reference each other through required foreign keys and cannot be ordered: "
                    + String.join(", ", stuck) + ". Make one FK nullable or DEFERRABLE, or create the rows in separate calls");
        }
        return order;
    }
}
