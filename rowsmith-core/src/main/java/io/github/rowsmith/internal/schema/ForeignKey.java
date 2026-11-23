package io.github.rowsmith.internal.schema;

import java.util.List;
import java.util.stream.Collectors;

public final class ForeignKey {
    public final String name;
    public final TableDef table;
    public final List<ColumnDef> columns;
    public final long refTableOid;
    public final List<Integer> refColumnNums;
    public final boolean deferrable;
    public final boolean initiallyDeferred;
    TableDef refTable;
    List<ColumnDef> refColumns;

    public ForeignKey(String name, TableDef table, List<ColumnDef> columns, long refTableOid, List<Integer> refColumnNums,
                      boolean deferrable, boolean initiallyDeferred) {
        this.name = name;
        this.table = table;
        this.columns = columns;
        this.refTableOid = refTableOid;
        this.refColumnNums = refColumnNums;
        this.deferrable = deferrable;
        this.initiallyDeferred = initiallyDeferred;
    }

    public TableDef refTable() { return refTable; }
    public List<ColumnDef> refColumns() { return refColumns; }

    public boolean required() {
        return columns.stream().anyMatch(ColumnDef::required);
    }

    public boolean isSelfReference() { return refTable == table; }

    public String columnList() {
        return columns.stream().map(c -> c.name).collect(Collectors.joining(", "));
    }

    public String describe() {
        return table.displayName() + "(" + columnList() + ") -> " + refTable.displayName() + "("
                + refColumns.stream().map(c -> c.name).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String toString() { return name + ": " + describe(); }
}
