package io.github.rowsmith.internal.plan;

import io.github.rowsmith.Row;
import io.github.rowsmith.ValueSource;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.ForeignKey;
import io.github.rowsmith.internal.schema.TableDef;
import io.github.rowsmith.internal.schema.UniqueKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Node {
    final int id;
    final TableDef table;
    final TableModel model;
    final String label;
    final Origin origin;
    final Node related;
    final ForeignKey via;
    private final Map<ColumnDef, Assignment> assignments = new HashMap<>();
    final Map<ColumnDef, ValueSource> sources = new HashMap<>();
    private final Map<ForeignKey, Ref> refs = new LinkedHashMap<>();
    final Set<ForeignKey> autoShared = new HashSet<>();
    final Set<ForeignKey> deferred = new HashSet<>();
    final List<Row> uses = new ArrayList<>();
    final Map<String, Row> parentRows = new LinkedHashMap<>();
    final Map<String, List<Row>> childRows = new LinkedHashMap<>();
    long number;
    boolean resolved;
    Row row;

    private static final Assignment DEFAULT = new Assignment.Default();
    private static final Set<ValueSource> CHOSEN_BY_ROWSMITH = EnumSet.of(ValueSource.ROWSMITH, ValueSource.DATABASE_DEFAULT, ValueSource.LEFT_NULL);

    enum Origin { REQUESTED, PARENT, CHILD }

    Node(int id, TableDef table, TableModel model, String label, Origin origin, Node related, ForeignKey via) {
        this.id = id;
        this.table = table;
        this.model = model;
        this.label = label;
        this.origin = origin;
        this.related = related;
        this.via = via;
    }

    String reason() {
        return switch (origin) {
            case REQUESTED -> "requested";
            case CHILD -> "child of " + related.label;
            case PARENT -> "parent of " + related.label + " via " + via.columnList();
        };
    }

    Node relParent() {
        return origin == Origin.CHILD ? related : null;
    }

    ForeignKey relFk() {
        return origin == Origin.CHILD ? via : null;
    }

    boolean isNumbered(ColumnDef c) {
        return sources.get(c) == ValueSource.ROWSMITH || sources.get(c) == ValueSource.SETUP;
    }

    boolean chosenByRowsmith(ColumnDef c) {
        return CHOSEN_BY_ROWSMITH.contains(sources.get(c));
    }

    List<String> numberedColumns(UniqueKey uk) {
        List<String> out = new ArrayList<>();
        for (ColumnDef c : table.columns()) {
            if (isNumbered(c) && uk.readColumns().contains(c.name)) out.add(c.name);
        }
        return out;
    }

    Assignment assignment(ColumnDef c) {
        return assignments.getOrDefault(c, DEFAULT);
    }

    void assign(ColumnDef c, Assignment a) {
        if (a instanceof Assignment.Default) assignments.remove(c);
        else assignments.put(c, a);
    }

    Map<ForeignKey, Ref> refs() {
        return Collections.unmodifiableMap(refs);
    }

    void link(ForeignKey fk, Ref ref) {
        refs.put(fk, ref);
        for (int i = 0; i < fk.columns.size(); i++) assignments.putIfAbsent(fk.columns.get(i), new Assignment.FromParent(fk, i));
    }

    void unlink(ForeignKey fk) {
        refs.remove(fk);
        assignments.values().removeIf(a -> a instanceof Assignment.FromParent p && p.fk() == fk);
    }

    Cell cell(ColumnDef c) {
        return cell(c, new HashSet<>());
    }

    private Cell cell(ColumnDef c, Set<Cell.Pending> seen) {
        Cell.Pending pending = new Cell.Pending(this, c);
        if (!seen.add(pending)) return pending;
        return switch (assignment(c)) {
            case Assignment.Value v -> new Cell.Known(v.value());
            case Assignment.Null() -> new Cell.Known(null);
            case Assignment.Preallocated p -> new Cell.Known(p.value());
            case Assignment.FromParent p -> referencedValue(p.fk(), p.index(), seen);
            case Assignment.Default() -> pending;
        };
    }

    private Cell referencedValue(ForeignKey fk, int i, Set<Cell.Pending> seen) {
        ColumnDef refCol = fk.refColumns().get(i);
        return switch (refs.get(fk)) {
            case Ref.Existing e -> new Cell.Known(e.row().values().get(refCol.name));
            case Ref.Planned p when p.node().row != null -> new Cell.Known(p.node().row.values().get(refCol.name));
            case Ref.Planned p -> p.node().cell(refCol, seen);
        };
    }

    boolean refersTo(ForeignKey fk, Node n) {
        return refs.get(fk) instanceof Ref.Planned(Node m) && m == n;
    }

    @Override
    public String toString() {
        return label;
    }
}
