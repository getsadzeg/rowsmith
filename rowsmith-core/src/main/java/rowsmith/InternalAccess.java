package rowsmith;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import rowsmith.internal.config.DateExpr;
import rowsmith.internal.plan.ApiAccess;
import rowsmith.internal.schema.Schema;

final class InternalAccess implements ApiAccess {

    @Override
    public Row row(String table, String qualifiedTable, Map<String, Object> values, List<String> primaryKey, Schema schema,
            Map<String, Row> parents, Map<String, List<Row>> children) {
        return new Row(table, qualifiedTable, values, primaryKey, schema, parents, children);
    }

    @Override
    public RowSpec spec(String table, Consumer<RowSpec> spec) {
        return RowSpec.of(table, spec);
    }

    @Override
    public List<Column<?>> typedColumns(RowSpec spec) {
        return spec.typedColumns();
    }

    @Override
    public DateExpr expr(RelativeTime time) {
        return time.expr();
    }
}
