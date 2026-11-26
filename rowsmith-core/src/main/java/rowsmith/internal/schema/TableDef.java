package rowsmith.internal.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rowsmith.internal.util.Strings;

public final class TableDef {
    public final long oid;
    public final String schema;
    public final String name;
    public final boolean isPartition;
    private final Map<String, ColumnDef> columns = new LinkedHashMap<>();
    final Map<Integer, ColumnDef> columnsByNum = new LinkedHashMap<>();
    public final List<ForeignKey> foreignKeys = new ArrayList<>();
    public final List<UniqueKey> uniqueKeys = new ArrayList<>();
    public final List<CheckConstraint> checks = new ArrayList<>();
    public Partitioning partitioning;
    UniqueKey primaryKey;
    String displayName;

    TableDef(long oid, String schema, String name, boolean isPartition) {
        this.oid = oid;
        this.schema = schema;
        this.name = name;
        this.isPartition = isPartition;
        this.displayName = schema + "." + name;
    }

    void addColumn(ColumnDef c) {
        c.table = this;
        columns.put(c.name, c);
        columnsByNum.put(c.num, c);
    }

    public ColumnDef column(String name) { return columns.get(name); }
    public ColumnDef columnByNum(int num) { return columnsByNum.get(num); }
    public List<ColumnDef> columns() { return Collections.unmodifiableList(new ArrayList<>(columns.values())); }
    public Set<String> columnNames() { return Collections.unmodifiableSet(columns.keySet()); }
    public UniqueKey primaryKey() { return primaryKey; }

    public String displayName() { return displayName; }
    public String qualifiedName() { return schema + "." + name; }
    public String sqlName() {
        return Strings.quoteIdent(schema) + "." + Strings.quoteIdent(name);
    }

    public List<ForeignKey> incomingForeignKeys(Schema schema) {
        List<ForeignKey> result = new ArrayList<>();
        for (TableDef t : schema.tables()) {
            for (ForeignKey fk : t.foreignKeys) if (fk.refTable == this) result.add(fk);
        }
        return result;
    }

    @Override
    public String toString() { return displayName; }
}
