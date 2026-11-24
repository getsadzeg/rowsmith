package io.github.rowsmith.internal.plan;

import io.github.rowsmith.Column;
import io.github.rowsmith.RelativeTime;
import io.github.rowsmith.Row;
import io.github.rowsmith.RowSpec;
import io.github.rowsmith.internal.config.DateExpr;
import io.github.rowsmith.internal.schema.Schema;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ApiAccess {

    Row row(String table, String qualifiedTable, Map<String, Object> values, List<String> primaryKey, Schema schema,
            Map<String, Row> parents, Map<String, List<Row>> children);

    RowSpec spec(String table, Consumer<RowSpec> spec);

    List<Column<?>> typedColumns(RowSpec spec);

    DateExpr expr(RelativeTime time);
}
