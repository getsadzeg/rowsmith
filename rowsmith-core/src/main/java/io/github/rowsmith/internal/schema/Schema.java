package io.github.rowsmith.internal.schema;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowsmithException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Schema {
    private final Map<Long, TableDef> byOid = new LinkedHashMap<>();
    private final Map<String, TableDef> byQualifiedName = new LinkedHashMap<>();
    private final List<String> searchPath;
    public final int serverVersionNum;

    Schema(List<String> searchPath, int serverVersionNum) {
        this.searchPath = searchPath;
        this.serverVersionNum = serverVersionNum;
    }

    void add(TableDef t) {
        byOid.put(t.oid, t);
        byQualifiedName.put(t.qualifiedName(), t);
    }

    TableDef byOid(long oid) { return byOid.get(oid); }

    public Collection<TableDef> tables() {
        List<TableDef> list = new ArrayList<>();
        for (TableDef t : byOid.values()) if (!t.isPartition) list.add(t);
        return list;
    }

    public List<String> searchPath() { return searchPath; }

    Collection<TableDef> allTables() { return byOid.values(); }

    public TableDef table(String name) {
        TableDef t = findTable(name);
        if (t != null) return t;
        List<String> names = new ArrayList<>();
        for (TableDef x : tables()) names.add(x.displayName());
        throw new RowsmithException(ErrorCode.UNKNOWN_TABLE,
                "unknown table '" + name + "'. Tables: " + String.join(", ", names.stream().sorted().toList()));
    }

    public TableDef findTable(String name) {
        String n = name.trim();
        if (n.startsWith("\"") && n.endsWith("\"")) n = n.substring(1, n.length() - 1);
        int dot = n.indexOf('.');
        if (dot > 0) {
            TableDef t = byQualifiedName.get(n);
            if (t != null && !t.isPartition) return t;
            return null;
        }
        for (String s : searchPath) {
            TableDef t = byQualifiedName.get(s + "." + n);
            if (t != null && !t.isPartition) return t;
        }
        return uniqueAcrossSchemas(n);
    }

    private TableDef uniqueAcrossSchemas(String n) {
        TableDef found = null;
        for (TableDef t : tables()) {
            if (t.name.equals(n)) {
                if (found != null) return null;
                found = t;
            }
        }
        return found;
    }

    public static ColumnDef column(TableDef table, String name) {
        ColumnDef c = table.column(name);
        if (c != null) return c;
        List<String> names = new ArrayList<>();
        table.columnNames().forEach(names::add);
        throw new RowsmithException(ErrorCode.UNKNOWN_COLUMN,
                "unknown column '" + table.displayName() + "." + name + "'. Columns: " + String.join(", ", names));
    }
}
