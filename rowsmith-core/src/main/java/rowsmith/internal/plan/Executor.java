package rowsmith.internal.plan;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import rowsmith.ErrorCode;
import rowsmith.Row;
import rowsmith.RowsmithException;
import rowsmith.internal.gen.Literals;
import rowsmith.internal.gen.TypeKind;
import rowsmith.internal.schema.CheckConstraint;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.ForeignKey;
import rowsmith.internal.schema.PgType;
import rowsmith.internal.schema.Schema;
import rowsmith.internal.schema.TableDef;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.util.Strings;

final class Executor {

    private final KeyRegistry keys;
    private final Schema schema;
    private final ApiAccess api;

    Executor(KeyRegistry keys, Schema schema, ApiAccess api) {
        this.keys = keys;
        this.schema = schema;
        this.api = api;
    }

    record Insert(Node node, List<ColumnDef> columns, List<Cell> values, boolean overriding) {
        String sql() {
            return statement(i -> "?::" + castFor(columns.get(i).type, columns.get(i))) + " RETURNING *";
        }

        String literalSql() {
            return statement(i -> switch (values.get(i)) {
                case Cell.Known k -> Literals.sql(k.value(), castFor(columns.get(i).type, columns.get(i)));
                case Cell other -> other.toString();
            }) + ";";
        }

        private String statement(IntFunction<String> value) {
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(node.table.sqlName());
            if (columns.isEmpty()) return sb.append(" DEFAULT VALUES").toString();
            sb.append(" (").append(columns.stream().map(c -> Strings.quoteIdent(c.name)).collect(Collectors.joining(", "))).append(")");
            if (overriding) sb.append(" OVERRIDING SYSTEM VALUE");
            return sb.append(" VALUES (").append(IntStream.range(0, columns.size()).mapToObj(value).collect(Collectors.joining(", "))).append(")").toString();
        }
    }

    static String castFor(PgType t, ColumnDef col) {
        if (t.isArray()) return castFor(t.element(), null) + "[]";
        if (!t.schema.equals("pg_catalog")) return t.castName;
        return switch (t.name) {
            case "bpchar" -> "bpchar";
            case "varchar" -> "varchar";
            case "bit" -> col != null ? col.formattedType : "varbit";
            default -> t.castName;
        };
    }

    Insert insertFor(Node n) {
        List<ColumnDef> cols = new ArrayList<>();
        List<Cell> vals = new ArrayList<>();
        boolean overriding = false;
        for (ColumnDef c : n.table.columns()) {
            if (n.assignment(c) instanceof Assignment.Default) continue;
            if (c.identity == ColumnDef.Identity.ALWAYS) overriding = true;
            cols.add(c);
            vals.add(n.cell(c));
        }
        return new Insert(n, cols, vals, overriding);
    }

    void execute(List<Node> order, Connection conn) throws SQLException {
        preallocate(order, conn);
        Set<String> deferred = new LinkedHashSet<>();
        Set<String> initiallyImmediate = new LinkedHashSet<>();
        for (Node n : order) {
            for (ForeignKey fk : n.deferred) {
                if (fk.isSelfReference() && n.refersTo(fk, n)) continue;
                String name = Strings.quoteIdent(fk.table.schema) + "." + Strings.quoteIdent(fk.name);
                deferred.add(name);
                if (!fk.initiallyDeferred) initiallyImmediate.add(name);
            }
        }
        setConstraints(conn, deferred, ConstraintMode.DEFERRED);
        try {
            insertAll(order, conn);
        } catch (SQLException | RuntimeException e) {
            try {
                setConstraints(conn, initiallyImmediate, ConstraintMode.IMMEDIATE);
            } catch (SQLException restoreFailure) {
                e.addSuppressed(restoreFailure);
            }
            throw e;
        }
        setConstraints(conn, initiallyImmediate, ConstraintMode.IMMEDIATE);
    }

    private enum ConstraintMode { DEFERRED, IMMEDIATE }

    private static void setConstraints(Connection conn, Set<String> constraints, ConstraintMode mode) throws SQLException {
        if (constraints.isEmpty()) return;
        try (Statement st = conn.createStatement()) {
            st.execute("SET CONSTRAINTS " + String.join(", ", constraints) + " " + mode.name());
        }
    }

    private void insertAll(List<Node> order, Connection conn) throws SQLException {
        for (Node n : order) {
            Map<UniqueKey, List<Object>> planned = keys.tuples(n);
            keys.checkKeys(n, true);
            n.row = insert(n, conn);
            Node relParent = n.relParent();
            if (relParent != null) relParent.childRows.computeIfAbsent(n.table.qualifiedName(), k -> new ArrayList<>()).add(n.row);
            keys.inserted(n, planned);
        }
        for (Node n : order) {
            for (Map.Entry<ForeignKey, Ref> e : n.refs().entrySet()) {
                Row parent = switch (e.getValue()) {
                    case Ref.Existing x -> x.row();
                    case Ref.Planned p -> p.node().row;
                };
                if (parent != null) n.parentRows.put(e.getKey().columns.size() == 1 ? e.getKey().columns.get(0).name : e.getKey().name, parent);
            }
        }
    }

    private void preallocate(List<Node> order, Connection conn) throws SQLException {
        for (Node n : order) {
            for (ForeignKey fk : n.deferred) {
                if (!(n.refs().get(fk) instanceof Ref.Planned(Node target))) continue;
                for (ColumnDef rc : fk.refColumns()) {
                    if (!(target.assignment(rc) instanceof Assignment.Default)) continue;
                    String next = rc.isIdentity() ? "nextval(" + Strings.quoteLiteral(rc.sequence) + "::regclass)" : rc.defaultExpr;
                    if (next == null || rc.isGenerated()) {
                        throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "cannot pre-compute " + rc.qualifiedName()
                                + " to insert the deferred foreign key " + fk.name + "; set it explicitly");
                    }
                    try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT (" + next + ")::" + castFor(rc.type, rc))) {
                        rs.next();
                        target.assign(rc, new Assignment.Preallocated(readValue(rs, 1, rc)));
                    }
                }
            }
        }
    }

    private Row insert(Node n, Connection conn) throws SQLException {
        Insert ins = insertFor(n);
        try (PreparedStatement ps = conn.prepareStatement(ins.sql())) {
            for (int i = 0; i < ins.values().size(); i++) {
                if (!(ins.values().get(i) instanceof Cell.Known value)) throw new IllegalStateException(n.label + " is inserted before the row it references");
                String text = Literals.toText(value.value());
                if (text == null) ps.setNull(i + 1, Types.OTHER);
                else ps.setObject(i + 1, text, Types.OTHER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return read(n, rs);
            }
        } catch (SQLException e) {
            throw translate(n, ins, e);
        }
    }

    private Row read(Node n, ResultSet rs) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String name = md.getColumnName(i);
            ColumnDef c = n.table.column(name);
            values.put(name, c == null ? rs.getObject(i) : readValue(rs, i, c));
        }
        List<String> pk = new ArrayList<>();
        if (n.table.primaryKey() != null) for (ColumnDef c : n.table.primaryKey().columns) pk.add(c.name);
        return api.row(n.table.displayName(), n.table.qualifiedName(), values, pk, schema, n.parentRows, n.childRows);
    }

    static Object readValue(ResultSet rs, int i, ColumnDef c) throws SQLException {
        return readValue(rs, i, c.type);
    }

    private static Object readValue(ResultSet rs, int i, PgType type) throws SQLException {
        TypeKind kind = TypeKind.of(type);
        return kind == TypeKind.ARRAY ? readArray(rs.getArray(i), type) : kind.read(rs, i);
    }

    private static List<Object> readArray(Array a, PgType type) throws SQLException {
        if (a == null) return null;
        try (ResultSet elements = a.getResultSet()) {
            PgType elementType = elements.getMetaData().getColumnType(2) == Types.ARRAY ? type : TypeKind.strip(type).element();
            List<Object> list = new ArrayList<>();
            while (elements.next()) list.add(readValue(elements, 2, elementType));
            return list;
        }
    }

    private RowsmithException translate(Node n, Insert ins, SQLException e) {
        ServerErrorMessage pg = e instanceof PSQLException p ? p.getServerErrorMessage() : null;
        String reason = pg == null ? e.getMessage() : pg.getMessage() + (pg.getDetail() == null ? "" : " (" + pg.getDetail() + ")");
        Map<String, Object> shown = new LinkedHashMap<>();
        for (int i = 0; i < ins.columns().size(); i++) {
            shown.put(ins.columns().get(i).name, ins.values().get(i) instanceof Cell.Known k ? k.value() : ins.values().get(i));
        }
        String where = "insert into " + n.table.displayName() + " (" + n.label + ", " + n.reason() + ") failed: ";
        ErrorCode code = errorCode(e.getSQLState());
        String hint = switch (code) {
            case UNIQUE_VIOLATION -> " Hint: another row already has these values; set the key columns in the test or with RowsmithSetup.value(...).";
            case FOREIGN_KEY_VIOLATION -> " Hint: link existing rows with set(\"fk_column\", row) or use(row) instead of raw ids.";
            case CHECK_VIOLATION -> checkHint(n.table, pg == null ? null : pg.getConstraint());
            default -> "";
        };
        return new RowsmithException(code, where + reason + "." + hint + " Values: " + shown, e);
    }

    private static String checkHint(TableDef t, String constraint) {
        for (CheckConstraint ch : t.checks) if (ch.name.equals(constraint)) return " Constraint: " + ch.definition() + ".";
        return "";
    }

    static ErrorCode errorCode(String sqlState) {
        PSQLState state = Arrays.stream(PSQLState.values()).filter(s -> s.getState().equals(sqlState)).findFirst().orElse(PSQLState.UNKNOWN_STATE);
        return switch (state) {
            case UNIQUE_VIOLATION -> ErrorCode.UNIQUE_VIOLATION;
            case FOREIGN_KEY_VIOLATION -> ErrorCode.FOREIGN_KEY_VIOLATION;
            case CHECK_VIOLATION -> ErrorCode.CHECK_VIOLATION;
            case NOT_NULL_VIOLATION -> ErrorCode.NOT_NULL_VIOLATION;
            case STRING_DATA_RIGHT_TRUNCATION, NUMERIC_VALUE_OUT_OF_RANGE, DATETIME_OVERFLOW -> ErrorCode.VALUE_OUT_OF_RANGE;
            case DATATYPE_MISMATCH -> ErrorCode.INVALID_VALUE;
            default -> isDataException(sqlState) ? ErrorCode.INVALID_VALUE : ErrorCode.DATABASE_ERROR;
        };
    }

    private static boolean isDataException(String sqlState) {
        String dataClass = PSQLState.DATA_ERROR.getState().substring(0, 2);
        return sqlState != null && sqlState.startsWith(dataClass);
    }
}
