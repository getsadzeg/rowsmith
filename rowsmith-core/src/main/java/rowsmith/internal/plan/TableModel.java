package rowsmith.internal.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rowsmith.internal.gen.ValueGenerator.Uniqueness;
import rowsmith.internal.schema.CheckConstraint;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.ForeignKey;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.sql.ColumnRules;
import rowsmith.internal.sql.Constants;
import rowsmith.internal.sql.SqlExpr;

final class TableModel {

    final Constants constants;
    final Map<String, ColumnRules> rules = new HashMap<>();
    final Map<ColumnDef, List<CheckConstraint>> domainChecks = new HashMap<>();
    final Set<ColumnDef> fkColumns = new HashSet<>();
    private final Map<ColumnDef, Uniqueness> uniqueness = new HashMap<>();
    final List<UniqueKey> keysWithoutFk = new ArrayList<>();

    TableModel(TableDef table, PgEval db) {
        List<SqlExpr> exprs = new ArrayList<>(ColumnRules.operands(table));
        for (ColumnDef c : table.columns()) if (c.stableDefault) exprs.add(c.defaultParsed);
        constants = db.constants(exprs);
        rules.putAll(ColumnRules.ofTable(table, constants));
        for (ColumnDef col : table.columns()) {
            List<CheckConstraint> dc = ColumnRules.domainChecks(col);
            if (!dc.isEmpty()) domainChecks.put(col, dc);
        }
        for (ForeignKey fk : table.foreignKeys) fkColumns.addAll(fk.columns);
        for (UniqueKey uk : table.uniqueKeys) {
            Set<ColumnDef> cols = new HashSet<>();
            for (String name : uk.readColumns()) cols.add(table.column(name));
            cols.remove(null);
            boolean onePartOnOneColumn = uk.parts.size() == 1 && cols.size() == 1;
            for (ColumnDef c : cols) uniqueness.merge(c, onePartOnOneColumn ? Uniqueness.ALONE : Uniqueness.PART_OF_KEY, TableModel::stricter);
            if (cols.stream().noneMatch(fkColumns::contains)) keysWithoutFk.add(uk);
        }
    }

    private static Uniqueness stricter(Uniqueness a, Uniqueness b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    Uniqueness uniqueness(ColumnDef c) {
        return uniqueness.getOrDefault(c, Uniqueness.NONE);
    }

    boolean generateOverDefault(ColumnDef c) {
        return uniqueness(c) == Uniqueness.ALONE && c.stableDefault;
    }

    ColumnRules rulesFor(ColumnDef c) {
        return rules.getOrDefault(c.name, ColumnRules.NONE);
    }

    boolean leaveNull(ColumnDef c) {
        if (c.required()) return false;
        return !rulesFor(c).notNull;
    }
}
