package rowsmith.internal.plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import rowsmith.ErrorCode;
import rowsmith.Row;
import rowsmith.RowsmithException;
import rowsmith.internal.gen.Literals;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.ForeignKey;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.util.Strings;

final class Cleanup {

    private final Engine engine;

    Cleanup(Engine engine) {
        this.engine = engine;
    }

    private record Target(TableDef table, List<ColumnDef> key, Row row) {
        List<Object> values(List<ColumnDef> columns) {
            return columns.stream().map(c -> row.values().get(c.name)).toList();
        }
    }

    void run() {
        List<Target> targets = targets();
        if (targets.isEmpty()) return;
        requireNoOpenTransaction();
        try (Connection conn = engine.dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<String> blockers = blockers(conn, targets);
                if (!blockers.isEmpty()) {
                    throw new RowsmithException(ErrorCode.CLEANUP_BLOCKED, "cleanup refused, nothing was deleted: rows Rowsmith did not create still reference rows it created: "
                            + Strings.joinLimited(blockers, 5) + ". Delete those rows first, or run the test inside a rolled-back transaction (@Transactional)");
                }
                delete(conn, targets);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RowsmithException(ErrorCode.DATABASE_ERROR, "cleanup failed and was rolled back, nothing was deleted: " + e.getMessage(), e);
        }
    }

    private List<Target> targets() {
        List<Target> targets = new ArrayList<>();
        for (Engine.Created c : engine.created) {
            if (c.externalTx()) continue;
            List<ColumnDef> key = keyOf(c.table());
            if (key.isEmpty()) {
                throw new RowsmithException(ErrorCode.CLEANUP_BLOCKED, "cannot clean up " + c.table().displayName()
                        + ": it has no primary key or NOT NULL unique key to identify rows by. Run the test in a rolled-back transaction instead");
            }
            targets.add(new Target(c.table(), key, c.row()));
        }
        return targets;
    }

    private void requireNoOpenTransaction() {
        boolean open;
        try {
            Connection c = engine.connections.get();
            try {
                open = engine.connections.isExternalTransaction(c);
            } finally {
                engine.connections.release(c);
            }
        } catch (SQLException e) {
            throw new RowsmithException(ErrorCode.DATABASE_ERROR, "cleanup failed, nothing was deleted: " + e.getMessage(), e);
        }
        if (open) {
            throw new RowsmithException(ErrorCode.CLEANUP_BLOCKED, "cleanup refused, nothing was deleted: a transaction is open on this thread."
                    + " Cleanup deletes on its own connection, so it cannot see that transaction's rows and would wait for its locks."
                    + " Call cleanup() after the transaction ends");
        }
    }

    private List<String> blockers(Connection conn, List<Target> targets) throws SQLException {
        Map<TableDef, List<Target>> byTable = new LinkedHashMap<>();
        for (Target t : targets) byTable.computeIfAbsent(t.table, k -> new ArrayList<>()).add(t);
        List<String> blockers = new ArrayList<>();
        for (Map.Entry<TableDef, List<Target>> parent : byTable.entrySet()) {
            for (ForeignKey fk : parent.getKey().incomingForeignKeys(engine.schema())) {
                blockers.addAll(referencing(conn, fk, parent.getValue(), byTable.getOrDefault(fk.table, List.of())));
            }
        }
        return blockers;
    }

    private static List<String> referencing(Connection conn, ForeignKey fk, List<Target> parents, List<Target> ownChildren) throws SQLException {
        List<List<Object>> refs = parents.stream().map(p -> p.values(fk.refColumns())).filter(v -> !v.contains(null)).toList();
        if (refs.isEmpty()) return List.of();
        List<ColumnDef> childKey = keyOf(fk.table);
        List<List<Object>> own = ownChildren.stream().map(c -> c.values(childKey)).toList();
        String shown = (childKey.isEmpty() ? fk.columns : childKey).stream().map(c -> Strings.quoteIdent(c.name) + "::text").collect(Collectors.joining(", "));
        String sql = "SELECT " + shown + " FROM " + fk.table.sqlName() + " WHERE " + in(fk.columns, refs.size())
                + (own.isEmpty() ? "" : " AND NOT " + in(childKey, own.size()));
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int next = bind(ps, refs, 1);
            bind(ps, own, next);
            List<String> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<String> shownValues = new ArrayList<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) shownValues.add(rs.getString(i));
                    out.add(fk.table.displayName() + " " + shownValues + " -> " + fk.refTable().displayName() + " via " + fk.columnList());
                }
            }
            return out;
        }
    }

    private static void delete(Connection conn, List<Target> targets) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("SET CONSTRAINTS ALL DEFERRED");
        }
        List<Target> childrenFirst = new ArrayList<>(targets);
        Collections.reverse(childrenFirst);
        for (Target t : childrenFirst) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + t.table.sqlName() + " WHERE " + in(t.key, 1))) {
                bind(ps, List.of(t.values(t.key)), 1);
                ps.executeUpdate();
            }
        }
    }

    private static List<ColumnDef> keyOf(TableDef t) {
        if (t.primaryKey() != null) return t.primaryKey().columns;
        for (UniqueKey uk : t.uniqueKeys) {
            if (!uk.isPartial() && uk.isPlainColumns() && uk.columns.stream().allMatch(ColumnDef::required)) return uk.columns;
        }
        return List.of();
    }

    private static String in(List<ColumnDef> cols, int rows) {
        String names = cols.stream().map(c -> Strings.quoteIdent(c.name)).collect(Collectors.joining(", ", "(", ")"));
        String row = cols.stream().map(c -> "?::" + Executor.castFor(c.type, c)).collect(Collectors.joining(", ", "(", ")"));
        return names + " IN (VALUES " + String.join(", ", Collections.nCopies(rows, row)) + ")";
    }

    private static int bind(PreparedStatement ps, List<List<Object>> rows, int from) throws SQLException {
        int i = from;
        for (List<Object> row : rows) {
            for (Object v : row) ps.setObject(i++, Literals.toText(v), Types.OTHER);
        }
        return i;
    }

    private static void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
        }
    }
}
