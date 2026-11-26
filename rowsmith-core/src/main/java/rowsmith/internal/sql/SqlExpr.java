package rowsmith.internal.sql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import rowsmith.internal.util.Strings;

public sealed interface SqlExpr {

    record Lit(Object value) implements SqlExpr {}

    record Col(String name) implements SqlExpr {}

    record Cast(SqlExpr expr, String type, String sqlType) implements SqlExpr {}

    enum UnaryOp { NOT, NEG }

    record Unary(UnaryOp op, SqlExpr expr) implements SqlExpr {}

    record Bin(Op op, SqlExpr left, SqlExpr right) implements SqlExpr {}

    record IsNull(SqlExpr expr, boolean negated) implements SqlExpr {}

    record IsBool(SqlExpr expr, boolean value, boolean negated) implements SqlExpr {}

    record Distinct(SqlExpr left, SqlExpr right, boolean negated) implements SqlExpr {}

    record AnyAll(Op op, SqlExpr left, SqlExpr array, boolean all) implements SqlExpr {}

    record InList(SqlExpr expr, List<SqlExpr> values, boolean negated) implements SqlExpr {}

    record Between(SqlExpr expr, SqlExpr low, SqlExpr high, boolean negated) implements SqlExpr {}

    record ArrayLit(List<SqlExpr> elements) implements SqlExpr {}

    record Subscript(SqlExpr array, SqlExpr index) implements SqlExpr {}

    record Func(String schema, String name, List<SqlExpr> args) implements SqlExpr {
        public Func(String name, List<SqlExpr> args) {
            this(null, name, args);
        }
    }

    record Keyword(String name) implements SqlExpr {}

    record Case(SqlExpr operand, List<SqlExpr> whens, List<SqlExpr> thens, SqlExpr otherwise) implements SqlExpr {}

    static Set<String> columnsOf(SqlExpr e) {
        Set<String> out = new LinkedHashSet<>();
        walk(e, x -> { if (x instanceof Col c) out.add(c.name()); });
        return out;
    }

    static Set<String> functionsOf(SqlExpr e) {
        Set<String> out = new LinkedHashSet<>();
        walk(e, x -> { if (x instanceof Func f) out.add(f.name()); });
        return out;
    }

    private static void walk(SqlExpr e, Consumer<SqlExpr> visit) {
        visit.accept(e);
        for (SqlExpr child : children(e)) walk(child, visit);
    }

    static List<SqlExpr> children(SqlExpr e) {
        return switch (e) {
            case Lit l -> List.of();
            case Keyword k -> List.of();
            case Col c -> List.of();
            case Cast c -> List.of(c.expr());
            case Unary u -> List.of(u.expr());
            case Bin b -> List.of(b.left(), b.right());
            case IsNull n -> List.of(n.expr());
            case IsBool n -> List.of(n.expr());
            case Distinct d -> List.of(d.left(), d.right());
            case AnyAll a -> List.of(a.left(), a.array());
            case InList i -> concat(List.of(i.expr()), i.values());
            case Between b -> List.of(b.expr(), b.low(), b.high());
            case ArrayLit a -> a.elements();
            case Subscript s -> List.of(s.array(), s.index());
            case Func f -> f.args();
            case Case c -> {
                List<SqlExpr> out = new ArrayList<>();
                if (c.operand() != null) out.add(c.operand());
                out.addAll(c.whens());
                out.addAll(c.thens());
                if (c.otherwise() != null) out.add(c.otherwise());
                yield out;
            }
        };
    }

    private static List<SqlExpr> concat(List<SqlExpr> a, List<SqlExpr> b) {
        List<SqlExpr> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    static SqlExpr unwrap(SqlExpr e) {
        while (e instanceof Cast c) e = c.expr();
        return e;
    }

    static String columnName(SqlExpr e) {
        return unwrap(e) instanceof Col c ? c.name() : null;
    }

    static String sql(SqlExpr e) {
        return switch (e) {
            case Lit l -> literal(l.value());
            case Keyword k -> k.name().toUpperCase(Locale.ROOT);
            case Col c -> Strings.quoteIdent(c.name());
            case Cast c -> "(" + sql(c.expr()) + ")::" + c.sqlType();
            case Unary u -> switch (u.op()) {
                case NOT -> "NOT ";
                case NEG -> "-";
            } + "(" + sql(u.expr()) + ")";
            case Bin b -> "(" + sql(b.left()) + ") " + b.op().sql + " (" + sql(b.right()) + ")";
            case IsNull n -> "(" + sql(n.expr()) + ") IS " + (n.negated() ? "NOT " : "") + "NULL";
            case IsBool b -> "(" + sql(b.expr()) + ") IS " + (b.negated() ? "NOT " : "") + (b.value() ? "TRUE" : "FALSE");
            case Distinct d -> "(" + sql(d.left()) + ") IS " + (d.negated() ? "NOT " : "") + "DISTINCT FROM (" + sql(d.right()) + ")";
            case AnyAll a -> "(" + sql(a.left()) + ") " + a.op().sql + (a.all() ? " ALL (" : " ANY (") + sql(a.array()) + ")";
            case InList i -> "(" + sql(i.expr()) + ")" + (i.negated() ? " NOT" : "") + " IN (" + list(i.values()) + ")";
            case Between b -> "(" + sql(b.expr()) + ")" + (b.negated() ? " NOT" : "") + " BETWEEN (" + sql(b.low()) + ") AND (" + sql(b.high()) + ")";
            case ArrayLit a -> "ARRAY[" + list(a.elements()) + "]";
            case Subscript s -> "(" + sql(s.array()) + ")[" + sql(s.index()) + "]";
            case Func f -> (f.schema() == null ? "" : ident(f.schema()) + ".") + ident(f.name()) + "(" + list(f.args()) + ")";
            case Case c -> {
                StringBuilder sb = new StringBuilder("CASE");
                if (c.operand() != null) sb.append(" (").append(sql(c.operand())).append(")");
                for (int i = 0; i < c.whens().size(); i++) {
                    sb.append(" WHEN (").append(sql(c.whens().get(i))).append(") THEN (").append(sql(c.thens().get(i))).append(")");
                }
                if (c.otherwise() != null) sb.append(" ELSE (").append(sql(c.otherwise())).append(")");
                yield sb.append(" END").toString();
            }
        };
    }

    private static String list(List<SqlExpr> es) {
        List<String> out = new ArrayList<>();
        for (SqlExpr x : es) out.add("(" + sql(x) + ")");
        return String.join(", ", out);
    }

    private static String ident(String name) {
        boolean plain = !name.isEmpty() && !Character.isDigit(name.charAt(0))
                && name.chars().allMatch(ch -> ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '_' || ch == '$');
        return plain ? name : Strings.quoteIdent(name);
    }

    private static String literal(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Boolean b) return b ? "TRUE" : "FALSE";
        if (v instanceof BigDecimal n) return n.toPlainString();
        // an escape string reads the same whatever standard_conforming_strings is
        return "E'" + v.toString().replace("\\", "\\\\").replace("'", "''") + "'";
    }
}
