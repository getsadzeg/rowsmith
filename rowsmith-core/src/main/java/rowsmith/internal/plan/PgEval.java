package rowsmith.internal.plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import rowsmith.ErrorCode;
import rowsmith.RowsmithException;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.sql.Constants;
import rowsmith.internal.sql.SqlExpr;
import rowsmith.internal.util.Savepoints;
import rowsmith.internal.util.Strings;

/**
 * A probe that raises an error in Postgres is answered as unknown, and the insert then reports the
 * database's error. Inside a transaction the probes run in a savepoint, so a failing probe does not abort it.
 */
final class PgEval {

    sealed interface Input {
        record Value(String text, String type) implements Input {}

        record Default(String expr, String type) implements Input {}
    }

    record Probe(SortedMap<String, Input> inputs, String expr) {}

    enum Truth { TRUE, FALSE, NULL, UNKNOWN }

    record Value(String text) {}

    private enum Kind { TRUTH, VALUE }

    private record Answer(Truth truth, Optional<Value> value) {
        static final Answer UNKNOWN = new Answer(Truth.UNKNOWN, Optional.empty());
    }

    private record Asked(Probe probe, Kind kind) {}

    static final int MAX_PARAMETERS = 65_535;

    @FunctionalInterface
    private interface RowReader {
        void read(ResultSet rs) throws SQLException;
    }

    private final Connection conn;
    private final ZoneId zone;
    private final Map<Asked, Answer> answers = new HashMap<>();

    PgEval(Connection conn, ZoneId zone) {
        this.conn = conn;
        this.zone = zone;
    }

    Map<Probe, Truth> truths(Collection<Probe> probes) {
        evaluate(probes, Kind.TRUTH);
        Map<Probe, Truth> out = new HashMap<>();
        for (Probe p : probes) out.put(p, answers.getOrDefault(new Asked(p, Kind.TRUTH), Answer.UNKNOWN).truth());
        return out;
    }

    Truth truth(Probe probe) {
        return truths(List.of(probe)).get(probe);
    }

    Map<Probe, Optional<Value>> values(Collection<Probe> probes) {
        evaluate(probes, Kind.VALUE);
        Map<Probe, Optional<Value>> out = new HashMap<>();
        for (Probe p : probes) out.put(p, answers.getOrDefault(new Asked(p, Kind.VALUE), Answer.UNKNOWN).value());
        return out;
    }

    Optional<Value> value(Probe probe) {
        return values(List.of(probe)).get(probe);
    }

    private void evaluate(Collection<Probe> probes, Kind kind) {
        List<Probe> chunk = new ArrayList<>();
        int params = 0;
        for (Probe p : new LinkedHashSet<>(probes)) {
            if (answers.containsKey(new Asked(p, kind))) continue;
            int n = parameters(p);
            if (params + n > MAX_PARAMETERS) {
                evaluateChunk(chunk, kind);
                chunk = new ArrayList<>();
                params = 0;
            }
            chunk.add(p);
            params += n;
        }
        if (!chunk.isEmpty()) evaluateChunk(chunk, kind);
    }

    private static int parameters(Probe p) {
        return (int) p.inputs().values().stream().filter(Input.Value.class::isInstance).count();
    }

    private void evaluateChunk(List<Probe> todo, Kind kind) {
        Map<List<String>, List<Integer>> shapes = new LinkedHashMap<>();
        for (int i = 0; i < todo.size(); i++) shapes.computeIfAbsent(shape(todo.get(i)), k -> new ArrayList<>()).add(i);
        StringBuilder sql = new StringBuilder();
        List<String> params = new ArrayList<>();
        for (List<Integer> ids : shapes.values()) {
            if (!sql.isEmpty()) sql.append("\nUNION ALL\n");
            select(sql, todo, ids, kind, params);
        }
        Map<Integer, Answer> got = new HashMap<>();
        boolean ok = run(sql.toString(), params, rs -> {
            boolean b = rs.getBoolean(2);
            Truth truth = rs.wasNull() ? Truth.NULL : b ? Truth.TRUE : Truth.FALSE;
            got.put(rs.getInt(1), new Answer(truth, Optional.of(new Value(rs.getString(3)))));
        });
        if (!ok && todo.size() > 1) {
            evaluateChunk(todo.subList(0, todo.size() / 2), kind);
            evaluateChunk(todo.subList(todo.size() / 2, todo.size()), kind);
            return;
        }
        for (int i = 0; i < todo.size(); i++) answers.put(new Asked(todo.get(i), kind), got.getOrDefault(i, Answer.UNKNOWN));
    }

    Set<Integer> existing(TableDef t, UniqueKey uk, List<List<Input>> keys) {
        String alias = idColumn(t.columnNames());
        List<String> names = new ArrayList<>();
        List<String> match = new ArrayList<>();
        for (int i = 0; i < uk.parts.size(); i++) {
            String name = Strings.quoteIdent("k" + i);
            names.add(name);
            String part = switch (uk.parts.get(i)) {
                case UniqueKey.ColumnPart c -> Strings.quoteIdent(c.column().name);
                case UniqueKey.ExprPart e -> "(" + e.text() + ")";
            };
            match.add(part + (uk.nullsNotDistinct ? " IS NOT DISTINCT FROM " : " = ") + alias + "." + name);
        }
        String where = String.join(" AND ", match) + (uk.isPartial() ? " AND (" + uk.predicate + ")" : "");
        Set<Integer> out = new HashSet<>();
        int perStatement = MAX_PARAMETERS / Math.max(1, uk.parts.size());
        for (int from = 0; from < keys.size(); from += perStatement) {
            StringBuilder sql = new StringBuilder("SELECT ").append(alias).append(".\"#\" FROM (VALUES ");
            List<String> params = new ArrayList<>();
            for (int k = from; k < Math.min(keys.size(), from + perStatement); k++) {
                sql.append(k > from ? ", (" : "(").append(k);
                for (Input in : keys.get(k)) {
                    sql.append(", ").append(slot(in));
                    if (in instanceof Input.Value v) params.add(v.text());
                }
                sql.append(")");
            }
            sql.append(") AS ").append(alias).append("(\"#\", ").append(String.join(", ", names)).append(") WHERE EXISTS (SELECT 1 FROM ")
                    .append(t.sqlName()).append(" WHERE ").append(where).append(")");
            try {
                Savepoints.isolated(conn, () -> query(sql.toString(), params, rs -> out.add(rs.getInt(1))));
            } catch (SQLException e) {
                throw new RowsmithException(ErrorCode.DATABASE_ERROR, "cannot look up the existing values of " + t.displayName() + " " + uk.describe()
                        + ": " + message(e), e);
            }
        }
        return out;
    }

    Constants constants(Collection<SqlExpr> exprs) {
        return Constants.read(conn, exprs);
    }

    Object read(String text, ColumnDef c) {
        AtomicReference<Object> value = new AtomicReference<>();
        try {
            inZone(() -> query("SELECT ?::" + Executor.castFor(c.baseType(), c), List.of(text), rs -> value.set(Executor.readValue(rs, 1, c))));
        } catch (SQLException e) {
            throw new RowsmithException(Executor.errorCode(e.getSQLState()), c.qualifiedName() + " = \"" + text + "\": " + message(e), e);
        }
        return value.get();
    }

    private static String message(SQLException e) {
        ServerErrorMessage pg = e instanceof PSQLException p ? p.getServerErrorMessage() : null;
        return pg == null ? e.getMessage() : pg.getMessage();
    }

    private void inZone(Savepoints.SqlWork<Void> work) throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        if (autoCommit) conn.setAutoCommit(false);
        try {
            Savepoints.undone(conn, () -> {
                try (Statement st = conn.createStatement()) {
                    st.execute("SET LOCAL TIME ZONE " + (zone.normalized() instanceof ZoneOffset o
                            ? "INTERVAL " + Strings.quoteLiteral(o.getTotalSeconds() + " seconds") : Strings.quoteLiteral(zone.getId())));
                }
                return work.run();
            });
        } finally {
            if (autoCommit) conn.setAutoCommit(true);
        }
    }

    private boolean run(String sql, List<String> params, RowReader reader) {
        try {
            Savepoints.isolated(conn, () -> query(sql, params, reader));
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private Void query(String sql, List<String> params, RowReader reader) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i) == null) ps.setNull(i + 1, Types.OTHER);
                else ps.setObject(i + 1, params.get(i), Types.OTHER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reader.read(rs);
            }
        }
        return null;
    }

    private static List<String> shape(Probe p) {
        List<String> out = new ArrayList<>();
        out.add(p.expr());
        p.inputs().forEach((name, in) -> out.add(name + " " + slot(in)));
        return out;
    }

    private static String slot(Input in) {
        return switch (in) {
            case Input.Value v -> "?::" + v.type();
            case Input.Default d -> "(" + d.expr() + ")::" + d.type();
        };
    }

    private static void select(StringBuilder sql, List<Probe> todo, List<Integer> ids, Kind kind, List<String> params) {
        Probe first = todo.get(ids.get(0));
        String id = idColumn(first.inputs().keySet());
        String expr = "(" + first.expr() + ")";
        sql.append("SELECT ").append(id).append(", ")
                .append(kind == Kind.TRUTH ? expr : "NULL::boolean").append(", ")
                .append(kind == Kind.VALUE ? expr + "::text" : "NULL::text")
                .append(" FROM (VALUES ");
        for (int k = 0; k < ids.size(); k++) {
            sql.append(k > 0 ? ", (" : "(").append(ids.get(k));
            for (Input in : todo.get(ids.get(k)).inputs().values()) {
                sql.append(", ").append(slot(in));
                if (in instanceof Input.Value v) params.add(v.text());
            }
            sql.append(")");
        }
        sql.append(") AS r(").append(id);
        for (String name : first.inputs().keySet()) sql.append(", ").append(Strings.quoteIdent(name));
        sql.append(")");
    }

    private static String idColumn(Set<String> inputs) {
        String id = "#";
        while (inputs.contains(id)) id += "#";
        return Strings.quoteIdent(id);
    }
}
