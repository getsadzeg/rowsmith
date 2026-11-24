package io.github.rowsmith.internal.plan;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.Row;
import io.github.rowsmith.RowSpec;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.internal.config.RowsmithClock;
import io.github.rowsmith.internal.gen.ValueGenerator;
import io.github.rowsmith.internal.schema.CatalogReader;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.ForeignKey;
import io.github.rowsmith.internal.schema.Schema;
import io.github.rowsmith.internal.schema.TableDef;
import io.github.rowsmith.internal.util.Savepoints;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;
import javax.sql.DataSource;

public final class Engine {

    private static final Map<DataSource, Schema> SCHEMAS = Collections.synchronizedMap(new WeakHashMap<>());

    final DataSource dataSource;
    final Connections connections;
    private final RowsmithClock clock;
    private final ValueGenerator generator;
    private final Map<TableDef, TableModel> models = new HashMap<>();
    private final List<ValuePattern> valuePatterns;
    private Map<ColumnDef, IntFunction<?>> patterns;
    private Schema schema;
    final GenState state = new GenState();
    final ApiAccess api;

    public Engine(DataSource dataSource, RowsmithClock clock, List<ValuePattern> valuePatterns, ApiAccess api) {
        this.dataSource = dataSource;
        this.api = api;
        this.valuePatterns = List.copyOf(valuePatterns);
        this.connections = Connections.of(dataSource);
        this.clock = clock;
        this.generator = new ValueGenerator(clock);
    }

    Schema schema() {
        if (schema == null) {
            schema = SCHEMAS.get(dataSource);
            if (schema == null) {
                schema = readCatalog();
                SCHEMAS.put(dataSource, schema);
            }
        }
        return schema;
    }

    private Schema readCatalog() {
        Connection c = null;
        try {
            c = connections.get();
            return CatalogReader.read(c);
        } catch (SQLException e) {
            throw new RowsmithException(ErrorCode.DATABASE_ERROR, "cannot read the Postgres catalog: " + e.getMessage(), e);
        } finally {
            if (c != null) connections.release(c);
        }
    }

    TableModel model(TableDef t, PgEval db) {
        return models.computeIfAbsent(t, k -> new TableModel(k, db));
    }

    IntFunction<?> pattern(ColumnDef c) {
        if (patterns == null) {
            Map<ColumnDef, IntFunction<?>> resolved = new HashMap<>();
            for (ValuePattern p : valuePatterns) {
                TableDef t = schema().table(p.table());
                ColumnDef col = Schema.column(t, p.column());
                if (col.isGenerated()) {
                    throw new RowsmithException(ErrorCode.INVALID_CONFIG, "setup value(" + col.qualifiedName()
                            + ", ...): a GENERATED column cannot be set");
                }
                for (ForeignKey fk : t.foreignKeys) {
                    if (fk.columns.contains(col)) {
                        throw new RowsmithException(ErrorCode.INVALID_CONFIG, "setup value(" + col.qualifiedName()
                                + ", ...): foreign key columns get their value from the referenced row; use use(...) in the test");
                    }
                }
                resolved.put(col, p.byRowNumber());
            }
            patterns = resolved;
        }
        return patterns.get(c);
    }

    RowsmithClock clock() { return clock; }
    ValueGenerator generator() { return generator; }

    public List<Row> create(List<RowSpec> specs) {
        Connection conn = null;
        boolean restoreAutoCommit = false;
        try {
            conn = connections.get();
            GenState attempt = state.copy();
            Planner planner = new Planner(this, attempt, new PgEval(conn, clock().zone()));
            List<Node> order = planner.plan(specs);
            Executor ex = new Executor(planner.keys, schema(), api);
            boolean external = connections.isExternalTransaction(conn);
            if (!external && conn.getAutoCommit()) {
                conn.setAutoCommit(false);
                restoreAutoCommit = true;
            }
            try {
                if (external) {
                    inSavepoint(ex, order, conn);
                } else {
                    ex.execute(order, conn);
                    conn.commit();
                }
            } catch (SQLException | RuntimeException e) {
                if (!external) Connections.rollbackQuietly(conn);
                for (Node n : order) n.row = null;
                throw e;
            }
            state.adopt(attempt);
            List<Row> roots = new ArrayList<>();
            for (Node n : planner.nodes) if (n.origin == Node.Origin.REQUESTED) roots.add(n.row);
            return roots;
        } catch (SQLException e) {
            throw new RowsmithException(ErrorCode.DATABASE_ERROR, "database error: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                if (restoreAutoCommit) Connections.enableAutoCommitQuietly(conn);
                connections.release(conn);
            }
        }
    }

    private static void inSavepoint(Executor ex, List<Node> order, Connection conn) throws SQLException {
        Savepoints.isolated(conn, () -> {
            ex.execute(order, conn);
            return null;
        });
    }
}
