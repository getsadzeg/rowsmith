package io.github.rowsmith.internal.sql;

import io.github.rowsmith.internal.gen.TypeKind;
import io.github.rowsmith.internal.util.Savepoints;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class Constants {

    private final Map<SqlExpr, Object> values;

    private Constants(Map<SqlExpr, Object> values) {
        this.values = values;
    }

    public Optional<Object> value(SqlExpr e) {
        return Optional.ofNullable(values.get(e));
    }

    public static Set<SqlExpr> operands(SqlExpr e) {
        Set<SqlExpr> out = new LinkedHashSet<>();
        if (e != null) collect(e, out);
        return out;
    }

    private static void collect(SqlExpr e, Set<SqlExpr> out) {
        if (SqlExpr.columnsOf(e).isEmpty()) return;
        for (SqlExpr child : SqlExpr.children(e)) {
            if (SqlExpr.columnsOf(child).isEmpty()) out.add(child);
            else collect(child, out);
        }
    }

    public static Constants read(Connection conn, Collection<SqlExpr> exprs) {
        Map<SqlExpr, Object> out = new HashMap<>();
        read(conn, List.copyOf(new LinkedHashSet<>(exprs)), out);
        return new Constants(out);
    }

    private static void read(Connection conn, List<SqlExpr> exprs, Map<SqlExpr, Object> out) {
        if (exprs.isEmpty()) return;
        try {
            out.putAll(Savepoints.isolated(conn, () -> select(conn, exprs)));
        } catch (SQLException e) {
            if (exprs.size() == 1) return;
            read(conn, exprs.subList(0, exprs.size() / 2), out);
            read(conn, exprs.subList(exprs.size() / 2, exprs.size()), out);
        }
    }

    private static Map<SqlExpr, Object> select(Connection conn, List<SqlExpr> exprs) throws SQLException {
        String sql = exprs.stream().map(e -> "(" + SqlExpr.sql(e) + ")").collect(Collectors.joining(", ", "SELECT ", ""));
        Map<SqlExpr, Object> out = new HashMap<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            for (int i = 0; i < exprs.size(); i++) {
                Object v = read(rs, i + 1);
                if (v != null) out.put(exprs.get(i), v);
            }
        }
        return out;
    }

    private static Object read(ResultSet rs, int i) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        if (md.getColumnType(i) != Types.ARRAY) {
            TypeKind kind = TypeKind.ofCatalogName(md.getColumnTypeName(i));
            return kind == TypeKind.NUMERIC ? numeric(rs.getObject(i)) : kind.read(rs, i);
        }
        Array a = rs.getArray(i);
        if (a == null) return null;
        try (ResultSet elements = a.getResultSet()) {
            List<Object> list = new ArrayList<>();
            while (elements.next()) list.add(read(elements, 2));
            return list;
        }
    }

    private static BigDecimal numeric(Object value) {
        return value instanceof BigDecimal b ? b : null;
    }
}
