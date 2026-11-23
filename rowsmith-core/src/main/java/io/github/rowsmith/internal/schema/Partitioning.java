package io.github.rowsmith.internal.schema;

import java.util.List;

public record Partitioning(List<ColumnDef> keyColumns, boolean anyKeyFits) {

    public boolean needsKey(ColumnDef c) {
        return !anyKeyFits && keyColumns.contains(c);
    }
}
