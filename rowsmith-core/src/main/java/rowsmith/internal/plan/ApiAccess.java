package rowsmith.internal.plan;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import rowsmith.Column;
import rowsmith.RelativeTime;
import rowsmith.Row;
import rowsmith.RowSpec;
import rowsmith.internal.config.DateExpr;
import rowsmith.internal.schema.Schema;

public interface ApiAccess {

    Row row(String table, String qualifiedTable, Map<String, Object> values, List<String> primaryKey, Schema schema,
            Map<String, Row> parents, Map<String, List<Row>> children);

    RowSpec spec(String table, Consumer<RowSpec> spec);

    List<Column<?>> typedColumns(RowSpec spec);

    DateExpr expr(RelativeTime time);
}
