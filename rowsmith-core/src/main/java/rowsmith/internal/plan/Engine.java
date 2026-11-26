package rowsmith.internal.plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;
import javax.sql.DataSource;
import rowsmith.ErrorCode;
import rowsmith.PgEnum;
import rowsmith.Plan;
import rowsmith.Row;
import rowsmith.RowSpec;
import rowsmith.RowsmithException;
import rowsmith.internal.config.RowsmithClock;
import rowsmith.internal.gen.Literals;
import rowsmith.internal.gen.ValueGenerator;
import rowsmith.internal.schema.CatalogReader;
import rowsmith.internal.schema.CheckConstraint;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.ForeignKey;
import rowsmith.internal.schema.Schema;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.util.Savepoints;
import rowsmith.internal.util.Strings;

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
    final List<Created> created = new ArrayList<>();

    record Created(Row row, TableDef table, boolean externalTx) {}

    public record Planned(List<Plan.PlannedRow> rows, List<String> sql) {}

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

    public Planned plan(List<RowSpec> specs) {
        Connection conn = null;
        try {
            conn = connections.get();
            Planner planner = new Planner(this, state.copy(), new PgEval(conn, clock().zone()));
            List<Node> order = planner.plan(specs);
            Executor ex = new Executor(planner.keys, schema(), api);
            List<Plan.PlannedRow> rows = new ArrayList<>();
            List<String> sql = new ArrayList<>();
            for (Node n : order) {
                rows.add(new Plan.PlannedRow(n.label, n.table.displayName(), Collections.unmodifiableMap(planner.displayValues(n)),
                        Collections.unmodifiableMap(planner.displaySources(n)), n.reason()));
                sql.add(ex.insertFor(n).literalSql());
            }
            return new Planned(rows, sql);
        } catch (SQLException e) {
            throw new RowsmithException(ErrorCode.DATABASE_ERROR, "database error: " + e.getMessage(), e);
        } finally {
            if (conn != null) connections.release(conn);
        }
    }

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
            for (Node n : order) created.add(new Created(n.row, n.table, external));
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

    public Row find(String table, Object id) {
        var t = schema().table(table);
        UniqueKey pk = t.primaryKey();
        if (pk == null || pk.columns.size() != 1) {
            throw new RowsmithException(ErrorCode.NO_SINGLE_PRIMARY_KEY, "find(\"" + table + "\", id) needs a single-column primary key");
        }
        var c = pk.columns.get(0);
        String idText = Literals.toText(id instanceof PgEnum e ? e.label() : id);
        Connection conn = null;
        try {
            conn = connections.get();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + t.sqlName() + " WHERE "
                    + Strings.quoteIdent(c.name) + " = ?::" + Executor.castFor(c.type, c))) {
                ps.setObject(1, idText, Types.OTHER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RowsmithException(ErrorCode.ROW_NOT_FOUND, "no " + t.displayName() + " row with " + c.name + " = " + idText);
                    }
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        String name = rs.getMetaData().getColumnName(i);
                        var col = t.column(name);
                        values.put(name, col == null ? rs.getObject(i) : Executor.readValue(rs, i, col));
                    }
                    return api.row(t.displayName(), t.qualifiedName(), values, List.of(c.name), schema(), Map.of(), Map.of());
                }
            }
        } catch (SQLException e) {
            throw new RowsmithException(Executor.errorCode(e.getSQLState()), "find failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) connections.release(conn);
        }
    }

    public List<Row> createdRows() {
        List<Row> out = new ArrayList<>();
        for (Created c : created) out.add(c.row());
        return out;
    }

    public void cleanup() {
        if (created.isEmpty()) return;
        new Cleanup(this).run();
        created.clear();
    }

    public String describe(String table) {
        var t = schema().table(table);
        StringBuilder sb = new StringBuilder("table ").append(t.displayName()).append('\n');
        for (var c : t.columns()) {
            sb.append("  ").append(c.name).append(' ').append(c.formattedType);
            if (c.required()) sb.append(" NOT NULL");
            if (c.isIdentity()) sb.append(" IDENTITY");
            else if (c.isGenerated()) sb.append(" GENERATED");
            else if (c.defaultExpr != null) sb.append(" DEFAULT ").append(c.defaultExpr);
            if (c.type.isEnum()) sb.append(" enum").append(c.type.enumLabels);
            sb.append('\n');
        }
        for (ForeignKey fk : t.foreignKeys) {
            sb.append("  fk ").append(fk.name).append(": (").append(fk.columnList()).append(") -> ").append(fk.refTable().displayName())
                    .append(fk.required() ? " required" : " optional").append(fk.deferrable ? " deferrable" : "").append('\n');
        }
        for (UniqueKey uk : t.uniqueKeys) sb.append("  ").append(uk.primary ? "pk " : "unique ").append(uk.describe()).append('\n');
        for (CheckConstraint ch : t.checks) sb.append("  check ").append(ch.name).append(": ").append(ch.body).append('\n');
        List<String> children = new ArrayList<>();
        for (ForeignKey fk : t.incomingForeignKeys(schema())) children.add(fk.table.displayName() + "." + fk.columnList());
        if (!children.isEmpty()) sb.append("  referenced by: ").append(String.join(", ", children)).append('\n');
        return sb.toString();
    }

    public List<String> tables() {
        List<String> out = new ArrayList<>();
        for (var t : schema().tables()) out.add(t.displayName());
        Collections.sort(out);
        return out;
    }
}
