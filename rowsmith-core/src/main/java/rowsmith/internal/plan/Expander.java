package rowsmith.internal.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import rowsmith.ErrorCode;
import rowsmith.Row;
import rowsmith.RowSpec;
import rowsmith.RowsmithException;
import rowsmith.ValueSource;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.ForeignKey;
import rowsmith.internal.schema.Schema;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.sql.SqlValues;

final class Expander {

    private final Engine engine;
    private final PgEval db;
    private final RowFiller filler;
    private final KeyRegistry keys;
    private final List<Node> nodes = new ArrayList<>();
    private final Deque<Node> stack = new ArrayDeque<>();
    private final Map<TableDef, Integer> labels = new HashMap<>();
    private final Map<Node, ForeignKey> resolving = new HashMap<>();

    Expander(Engine engine, PgEval db, RowFiller filler, KeyRegistry keys) {
        this.engine = engine;
        this.db = db;
        this.filler = filler;
        this.keys = keys;
    }

    List<Node> expand(List<RowSpec> roots) {
        for (RowSpec spec : roots) expand(spec, Node.Origin.REQUESTED, null, null, List.of());
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (!n.resolved) resolve(n);
        }
        return nodes;
    }

    private Node newNode(TableDef t, Node.Origin origin, Node related, ForeignKey via) {
        int k = labels.merge(t, 1, Integer::sum);
        Node n = new Node(nodes.size() + 1, t, engine.model(t, db), t.displayName() + "#" + k, origin, related, via);
        nodes.add(n);
        return n;
    }

    private Node expand(RowSpec spec, Node.Origin origin, Node related, ForeignKey via, List<Row> inheritedUses) {
        TableDef t = engine.schema().table(spec.table());
        for (var col : engine.api.typedColumns(spec)) {
            if (!col.table().qualifiedName().equals(t.qualifiedName())) {
                throw new RowsmithException(ErrorCode.UNKNOWN_COLUMN, col + " is not a column of " + t.displayName());
            }
        }
        Node n = newNode(t, origin, related, via);
        n.uses.addAll(inheritedUses);
        n.uses.addAll(spec.uses());
        n.ownUses.addAll(spec.uses());
        for (Map.Entry<String, Row> e : spec.usesByColumn().entrySet()) RowFiller.useRow(n, Schema.column(n.table, e.getKey()), e.getValue());
        for (Map.Entry<String, Object> e : spec.values().entrySet()) filler.applyValue(n, e.getKey(), e.getValue(), ValueSource.USER);
        if (origin == Node.Origin.PARENT && via.columns.size() > 1) inheritKeyFromChild(n);
        filler.generate(n);
        for (RowSpec.Child child : spec.children()) {
            TableDef ct = engine.schema().table(child.table());
            ForeignKey fk = childFk(ct, t, child.via());
            if (child.count() > 1 && isUniqueAlone(ct, fk)) {
                throw new RowsmithException(ErrorCode.UNIQUE_VIOLATION, "with(\"" + child.table() + "\", " + child.count() + "): " + ct.displayName()
                        + "(" + fk.columnList() + ") is unique, so a " + t.displayName() + " can have at most one " + ct.displayName());
            }
            for (int i = 0; i < child.count(); i++) expand(engine.api.spec(child.table(), child.spec()), Node.Origin.CHILD, n, fk, n.uses);
        }
        return n;
    }

    private void inheritKeyFromChild(Node parent) {
        ForeignKey fk = parent.via;
        Node child = parent.related;
        for (int i = 0; i < fk.columns.size(); i++) {
            ColumnDef childColumn = fk.columns.get(i);
            ColumnDef parentColumn = fk.refColumns().get(i);
            if (child.assignment(childColumn) instanceof Assignment.Value v) filler.applyValue(parent, parentColumn.name, v.value(), ValueSource.USER);
            else parent.inheritRules(parentColumn, child.rulesFor(childColumn));
        }
    }

    private static boolean isUniqueAlone(TableDef t, ForeignKey fk) {
        for (UniqueKey uk : t.uniqueKeys) {
            if (uk.isPartial()) continue;
            if (uk.isPlainColumns() && uk.columns.size() == fk.columns.size() && new HashSet<>(uk.columns).equals(new HashSet<>(fk.columns))) return true;
        }
        return false;
    }

    private static ForeignKey childFk(TableDef child, TableDef parent, String via) {
        List<ForeignKey> fks = new ArrayList<>();
        for (ForeignKey fk : child.foreignKeys) if (fk.refTable() == parent) fks.add(fk);
        if (via != null) {
            for (ForeignKey fk : fks) if (fk.columns.size() == 1 && fk.columns.get(0).name.equals(via) || fk.name.equals(via)) return fk;
            throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, child.displayName() + "." + via + " is not a foreign key to " + parent.displayName()
                    + (fks.isEmpty() ? "" : "; candidates: " + describe(fks)));
        }
        if (fks.isEmpty()) {
            throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, child.displayName() + " has no foreign key to " + parent.displayName()
                    + ", so with(\"" + child.displayName() + "\", ...) cannot attach it to a " + parent.displayName() + " row"
                    + outgoing(child));
        }
        if (fks.size() > 1) {
            throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, child.displayName() + " has " + fks.size() + " foreign keys to " + parent.displayName()
                    + " (" + describe(fks) + "); say which one: with(\"" + child.displayName() + "\", \"" + fks.get(0).columns.get(0).name + "\", n, spec)");
        }
        return fks.get(0);
    }

    private static String outgoing(TableDef t) {
        if (t.foreignKeys.isEmpty()) return "";
        List<String> s = new ArrayList<>();
        for (ForeignKey fk : t.foreignKeys) s.add(fk.columnList() + " -> " + fk.refTable().displayName());
        return " (its foreign keys: " + String.join(", ", s) + ")";
    }

    private static String describe(Collection<ForeignKey> fks) {
        List<String> s = new ArrayList<>();
        for (ForeignKey fk : fks) s.add(fk.columnList());
        return String.join(", ", s);
    }

    private void resolve(Node n) {
        n.resolved = true;
        stack.push(n);
        try {
            for (ForeignKey fk : n.table.foreignKeys) resolveFk(n, fk);
        } finally {
            stack.pop();
        }
        unshareParentsThatDuplicateKeys(n);
        keys.checkKeys(n, true);
        keys.register(n, true);
    }

    private void unshareParentsThatDuplicateKeys(Node n) {
        for (List<UniqueKey> dups = keys.duplicateKeys(n, true); !dups.isEmpty(); dups = keys.duplicateKeys(n, true)) {
            ForeignKey replace = null;
            for (ForeignKey fk : n.table.foreignKeys) {
                if (n.autoShared.contains(fk) && dups.get(0).columns.stream().anyMatch(fk.columns::contains)) { replace = fk; break; }
            }
            if (replace == null) break;
            n.autoShared.remove(replace);
            stack.push(n);
            try {
                n.link(replace, new Ref.Planned(autoParent(replace, n)));
            } finally {
                stack.pop();
            }
        }
    }

    private void resolveFk(Node n, ForeignKey fk) {
        if (fk == n.relFk()) {
            if (setsPartOfKey(n, fk)) throw withChildSetsPartOfKey(n, fk);
            n.link(fk, new Ref.Planned(n.related));
            return;
        }
        if (n.refs().containsKey(fk)) {
            if (setsPartOfKey(n, fk)) throw partOfKey(n, fk);
            return;
        }
        if (specSetsWholeKey(n, fk)) return;
        for (Row r : n.uses) {
            if (r.qualifiedTable().equals(fk.refTable().qualifiedName()) && matchesSetValues(n, fk, c -> new Cell.Known(r.values().get(c.name)))) {
                n.link(fk, new Ref.Existing(r));
                return;
            }
        }
        for (Row r : n.ownUses) if (r.qualifiedTable().equals(fk.refTable().qualifiedName())) throw useConflict(n, fk, r);
        if (!fk.required()) return;
        if (setsPartOfKey(n, fk)) {
            if (fk.isSelfReference() || inProgress(fk) != null || newParentWouldRepeatAKey(n, fk)) throw partOfKey(n, fk);
        } else if (linkedWithoutNewParent(n, fk)) {
            return;
        }
        resolving.put(n, fk);
        try {
            n.link(fk, new Ref.Planned(autoParent(fk, n)));
        } finally {
            resolving.remove(n);
        }
    }

    private boolean linkedWithoutNewParent(Node n, ForeignKey fk) {
        Node shared = findShared(n, fk);
        if (shared != null) {
            n.link(fk, new Ref.Planned(shared));
            n.autoShared.add(fk);
            return true;
        }
        if (fk.isSelfReference()) {
            n.link(fk, new Ref.Planned(n));
            n.deferred.add(fk);
            return true;
        }
        Node inProgress = inProgress(fk);
        if (inProgress == null) return false;
        if (fk.deferrable) {
            n.link(fk, new Ref.Planned(inProgress));
            n.deferred.add(fk);
            return true;
        }
        if (deferAncestorEdge(n, inProgress)) {
            n.link(fk, new Ref.Planned(inProgress));
            return true;
        }
        throw cycleError(fk);
    }

    private Node inProgress(ForeignKey fk) {
        for (Node s : stack) if (s.table == fk.refTable()) return s;
        return null;
    }

    private static boolean setsPartOfKey(Node n, ForeignKey fk) {
        return fk.columns.size() > 1 && !specSetsWholeKey(n, fk) && fk.columns.stream().anyMatch(c -> n.assignment(c) instanceof Assignment.Value);
    }

    private static boolean newParentWouldRepeatAKey(Node n, ForeignKey fk) {
        Set<ColumnDef> given = new HashSet<>();
        for (int i = 0; i < fk.columns.size(); i++) if (n.assignment(fk.columns.get(i)) instanceof Assignment.Value) given.add(fk.refColumns().get(i));
        if (given.stream().anyMatch(c -> c.isIdentity() || c.drawsFromSequence())) return true;
        return fk.refTable().uniqueKeys.stream().anyMatch(uk -> !uk.isPartial() && uk.isPlainColumns() && given.containsAll(uk.columns));
    }

    private static List<String> setKeyColumns(Node n, ForeignKey fk) {
        return fk.columns.stream().filter(c -> n.assignment(c) instanceof Assignment.Value).map(c -> c.name).toList();
    }

    private static RowsmithException partOfKey(Node n, ForeignKey fk) {
        return new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, n.table.displayName() + " sets only " + String.join(", ", setKeyColumns(n, fk))
                + " of key (" + fk.columnList() + "); set the whole key, or use(...) a matching " + fk.refTable().displayName());
    }

    private static RowsmithException withChildSetsPartOfKey(Node n, ForeignKey fk) {
        List<String> parentColumns = new ArrayList<>();
        for (int i = 0; i < fk.columns.size(); i++) if (n.assignment(fk.columns.get(i)) instanceof Assignment.Value) parentColumns.add(fk.refColumns().get(i).name);
        return new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, n.table.displayName() + " sets only " + String.join(", ", setKeyColumns(n, fk))
                + " of key (" + fk.columnList() + "), which with(...) fills from " + n.related.label + "; set " + String.join(", ", parentColumns)
                + " on the " + fk.refTable().displayName() + " instead");
    }

    private boolean deferAncestorEdge(Node n, Node inProgress) {
        for (Node s : stack) {
            if (s == n) continue;
            ForeignKey up = resolving.get(s);
            if (up != null && up.deferrable) {
                s.deferred.add(up);
                return true;
            }
            if (s == inProgress) return false;
        }
        return false;
    }

    private static boolean specSetsWholeKey(Node n, ForeignKey fk) {
        return fk.columns.stream().anyMatch(c -> n.assignment(c) instanceof Assignment.Null)
                || fk.columns.stream().allMatch(c -> n.assignment(c) instanceof Assignment.Value);
    }

    private static boolean matchesSetValues(Node n, ForeignKey fk, Function<ColumnDef, Cell> parentCell) {
        for (int i = 0; i < fk.columns.size(); i++) {
            if (!(n.assignment(fk.columns.get(i)) instanceof Assignment.Value v)) continue;
            if (!(parentCell.apply(fk.refColumns().get(i)) instanceof Cell.Known(Object p)) || !Boolean.TRUE.equals(SqlValues.equal(v.value(), p))) return false;
        }
        return true;
    }

    private static RowsmithException useConflict(Node n, ForeignKey fk, Row used) {
        for (int i = 0; i < fk.columns.size(); i++) {
            ColumnDef childColumn = fk.columns.get(i);
            ColumnDef parentColumn = fk.refColumns().get(i);
            if (!(n.assignment(childColumn) instanceof Assignment.Value v)) continue;
            Object p = used.values().get(parentColumn.name);
            if (Boolean.TRUE.equals(SqlValues.equal(v.value(), p))) continue;
            return new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, "use(" + rowLabel(fk.refTable(), used) + ") has " + parentColumn.name + " = " + p
                    + ", but " + n.table.displayName() + "." + childColumn.name + " is set to " + v.value() + "; drop one of them");
        }
        throw new IllegalStateException("no conflicting column on " + fk.describe());
    }

    private static String rowLabel(TableDef t, Row r) {
        UniqueKey pk = t.primaryKey();
        if (pk == null) return t.displayName() + r.values();
        List<String> ids = new ArrayList<>();
        for (ColumnDef c : pk.columns) ids.add(String.valueOf(r.values().get(c.name)));
        return t.displayName() + "#" + String.join(",", ids);
    }

    private Node autoParent(ForeignKey fk, Node child) {
        Node p = expand(engine.api.spec(fk.refTable().displayName(), null), Node.Origin.PARENT, child, fk, child.uses);
        resolve(p);
        return p;
    }

    private RowsmithException cycleError(ForeignKey fk) {
        List<String> path = new ArrayList<>();
        List<Node> chain = new ArrayList<>(stack);
        Collections.reverse(chain);
        boolean started = false;
        for (Node s : chain) {
            if (s.table == fk.refTable()) started = true;
            if (started) path.add(s.table.displayName());
        }
        path.add(fk.refTable().displayName());
        return new RowsmithException(ErrorCode.REQUIRED_FK_CYCLE, "required foreign keys form a cycle: " + String.join(" -> ", path)
                + ". None of them is nullable or DEFERRABLE, so no insert order exists. Make one FK nullable or DEFERRABLE"
                + " INITIALLY DEFERRED, or pass an existing row with use(...) for " + fk.describe());
    }

    private Node findShared(Node n, ForeignKey fk) {
        if (fk.columns.size() > 1 && fk.columns.stream().anyMatch(c -> !n.rulesFor(c).isEmpty())) return null;
        TableDef target = fk.refTable();
        for (Node m : nodes) {
            if (m == n || m.table != target || stack.contains(m) || !m.resolved) continue;
            if (dependsOn(m, n, new HashSet<>())) continue;
            if (sharingKeepsKeysUnique(n, fk, m)) return m;
        }
        return null;
    }

    private boolean sharingKeepsKeysUnique(Node n, ForeignKey fk, Node cand) {
        n.link(fk, new Ref.Planned(cand));
        try {
            for (UniqueKey uk : n.table.uniqueKeys) {
                if (uk.columns.stream().noneMatch(fk.columns::contains)) continue;
                if (keys.keyTuple(n, uk).filter(t -> KeyRegistry.heldByAnother(keys.owner(uk, t), n)).isPresent()) return false;
            }
            return true;
        } finally {
            n.unlink(fk);
        }
    }

    private static boolean dependsOn(Node a, Node b, Set<Node> seen) {
        if (a == b) return true;
        if (!seen.add(a)) return false;
        for (Ref r : a.refs().values()) if (r instanceof Ref.Planned(Node m) && dependsOn(m, b, seen)) return true;
        return a.relParent() != null && dependsOn(a.relParent(), b, seen);
    }
}
