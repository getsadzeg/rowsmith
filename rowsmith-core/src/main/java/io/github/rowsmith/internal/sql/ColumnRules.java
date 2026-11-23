package io.github.rowsmith.internal.sql;

import io.github.rowsmith.internal.sql.SqlExpr.*;
import io.github.rowsmith.internal.schema.CheckConstraint;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.TableDef;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.Set;

public final class ColumnRules {
    public static final String DOMAIN_VALUE = "value";

    public final List<Object> allowed;
    public final String allowedSource;
    public final Set<Object> excluded;
    public final Object lo;
    public final boolean loInclusive;
    public final Object hi;
    public final boolean hiInclusive;
    public final Integer minLen;
    public final Integer maxLen;
    public final List<String> likes;
    public final List<String> regexes;
    public final boolean notNull;
    public final JsonKind jsonKind;
    public final TruncUnit truncUnit;
    public final LetterCase letterCase;

    public enum LetterCase { LOWER, UPPER }

    public enum JsonKind {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL;

        static Optional<JsonKind> of(String jsonTypeof) {
            return Arrays.stream(values()).filter(k -> k.name().equalsIgnoreCase(jsonTypeof)).findFirst();
        }
    }

    public enum TruncUnit {
        YEAR, QUARTER, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND;

        static Optional<TruncUnit> of(String dateTruncField) {
            return Arrays.stream(values()).filter(u -> u.name().equalsIgnoreCase(dateTruncField)).findFirst();
        }
    }

    public static final ColumnRules NONE = new ColumnRules();

    private static final class Draft {
        List<Object> allowed;
        String allowedSource;
        final Set<Object> excluded = new LinkedHashSet<>();
        Object lo;
        boolean loInclusive = true;
        Object hi;
        boolean hiInclusive = true;
        Integer minLen;
        Integer maxLen;
        final List<String> likes = new ArrayList<>();
        final List<String> regexes = new ArrayList<>();
        boolean notNull;
        JsonKind jsonKind;
        TruncUnit truncUnit;
        LetterCase letterCase;
    }

    public ColumnRules() {
        this(new Draft());
    }

    private ColumnRules(Draft d) {
        this.allowed = d.allowed == null ? null : Collections.unmodifiableList(new ArrayList<>(d.allowed));
        this.allowedSource = d.allowedSource;
        this.excluded = Collections.unmodifiableSet(new LinkedHashSet<>(d.excluded));
        this.lo = d.lo;
        this.loInclusive = d.loInclusive;
        this.hi = d.hi;
        this.hiInclusive = d.hiInclusive;
        this.minLen = d.minLen;
        this.maxLen = d.maxLen;
        this.likes = List.copyOf(d.likes);
        this.regexes = List.copyOf(d.regexes);
        this.notNull = d.notNull;
        this.jsonKind = d.jsonKind;
        this.truncUnit = d.truncUnit;
        this.letterCase = d.letterCase;
    }

    private static ColumnRules rule(Consumer<Draft> facts) {
        Draft d = new Draft();
        facts.accept(d);
        return new ColumnRules(d);
    }

    public static ColumnRules allowed(List<?> values, String source) {
        return rule(d -> {
            d.allowed = new ArrayList<>(values);
            d.allowedSource = source;
        });
    }

    public boolean isEmpty() {
        return allowed == null && excluded.isEmpty() && lo == null && hi == null && minLen == null && maxLen == null
                && likes.isEmpty() && regexes.isEmpty() && !notNull && jsonKind == null && truncUnit == null && letterCase == null;
    }

    public ColumnRules and(ColumnRules o) {
        Draft d = new Draft();
        d.allowed = allowed == null ? o.allowed : o.allowed == null ? allowed : intersect(allowed, o.allowed);
        d.allowedSource = allowed == null ? o.allowedSource : allowedSource;
        d.excluded.addAll(excluded);
        d.excluded.addAll(o.excluded);
        Integer lower = lo == null || o.lo == null ? null : SqlValues.compare(o.lo, lo);
        boolean otherLo = o.lo != null && (lo == null || (lower != null && (lower > 0 || (lower == 0 && !o.loInclusive))));
        d.lo = otherLo ? o.lo : lo;
        d.loInclusive = otherLo ? o.loInclusive : loInclusive;
        Integer upper = hi == null || o.hi == null ? null : SqlValues.compare(o.hi, hi);
        boolean otherHi = o.hi != null && (hi == null || (upper != null && (upper < 0 || (upper == 0 && !o.hiInclusive))));
        d.hi = otherHi ? o.hi : hi;
        d.hiInclusive = otherHi ? o.hiInclusive : hiInclusive;
        d.minLen = o.minLen == null ? minLen : max(minLen, o.minLen);
        d.maxLen = o.maxLen == null ? maxLen : min(maxLen, o.maxLen);
        d.likes.addAll(likes);
        d.likes.addAll(o.likes);
        d.regexes.addAll(regexes);
        d.regexes.addAll(o.regexes);
        d.notNull = notNull || o.notNull;
        d.jsonKind = jsonKind == null ? o.jsonKind : jsonKind;
        d.truncUnit = truncUnit == null ? o.truncUnit : truncUnit;
        d.letterCase = letterCase == null ? o.letterCase : letterCase;
        return new ColumnRules(d);
    }

    private static List<Object> intersect(List<Object> a, List<Object> b) {
        List<Object> keep = new ArrayList<>();
        for (Object x : a) {
            for (Object v : b) if (Boolean.TRUE.equals(SqlValues.equal(x, v))) { keep.add(x); break; }
        }
        return keep;
    }

    public static Map<String, ColumnRules> ofTable(TableDef table, Constants k) {
        Map<String, ColumnRules> out = new HashMap<>();
        for (CheckConstraint c : table.checks) extract(c.expr, c.name, k).forEach((col, r) -> add(out, col, r));
        for (ColumnDef col : table.columns()) {
            Map<String, ColumnRules> domain = new HashMap<>();
            for (CheckConstraint c : domainChecks(col)) extract(c.expr, c.name, k).forEach((name, r) -> add(domain, name, r));
            ColumnRules r = domain.get(DOMAIN_VALUE);
            if (r != null) add(out, col.name, r);
        }
        return out;
    }

    public static Set<SqlExpr> operands(TableDef table) {
        Set<SqlExpr> out = new LinkedHashSet<>();
        for (CheckConstraint c : table.checks) out.addAll(Constants.operands(c.expr));
        for (ColumnDef col : table.columns()) for (CheckConstraint c : domainChecks(col)) out.addAll(Constants.operands(c.expr));
        return out;
    }

    public static List<CheckConstraint> domainChecks(ColumnDef col) {
        return col.type.domainChain().flatMap(t -> t.domainChecks.stream()).toList();
    }

    public static Map<String, ColumnRules> extract(SqlExpr expr, String source, Constants k) {
        Map<String, ColumnRules> out = new HashMap<>();
        if (expr != null) for (SqlExpr c : conjuncts(expr)) extractOne(c, source, out, k);
        return out;
    }

    public static List<SqlExpr> conjuncts(SqlExpr e) {
        List<SqlExpr> list = new ArrayList<>();
        if (e instanceof Unary u && u.op() == UnaryOp.NOT) {
            SqlExpr n = negate(u.expr());
            if (n != null) e = n;
        }
        if (e instanceof Bin b && b.op() == Op.AND) {
            list.addAll(conjuncts(b.left()));
            list.addAll(conjuncts(b.right()));
        } else {
            list.add(e);
        }
        return list;
    }

    private static List<SqlExpr> disjuncts(SqlExpr e) {
        List<SqlExpr> list = new ArrayList<>();
        if (e instanceof Unary u && u.op() == UnaryOp.NOT) {
            SqlExpr n = negate(u.expr());
            if (n != null) e = n;
        }
        if (e instanceof Bin b && b.op() == Op.OR) {
            list.addAll(disjuncts(b.left()));
            list.addAll(disjuncts(b.right()));
        } else {
            list.add(e);
        }
        return list;
    }

    private static SqlExpr negate(SqlExpr e) {
        return switch (e) {
            case Unary u when u.op() == UnaryOp.NOT -> u.expr();
            case Bin b when b.op().isComparison() || b.op().isPattern() -> new Bin(b.op().negate(), b.left(), b.right());
            case Bin b when b.op() == Op.AND || b.op() == Op.OR -> {
                SqlExpr l = negate(b.left());
                SqlExpr r = negate(b.right());
                yield l == null || r == null ? null : new Bin(b.op() == Op.AND ? Op.OR : Op.AND, l, r);
            }
            case IsNull n -> new IsNull(n.expr(), !n.negated());
            case IsBool b -> new IsBool(b.expr(), b.value(), !b.negated());
            case InList i -> new InList(i.expr(), i.values(), !i.negated());
            case Between b -> new Between(b.expr(), b.low(), b.high(), !b.negated());
            case Distinct d -> new Distinct(d.left(), d.right(), !d.negated());
            case Col c -> new Bin(Op.EQ, c, new Lit(Boolean.FALSE));
            default -> null;
        };
    }

    private static void add(Map<String, ColumnRules> out, String col, ColumnRules r) {
        out.merge(col, r, ColumnRules::and);
    }

    private static void extractOne(SqlExpr e, String source, Map<String, ColumnRules> out, Constants k) {
        switch (e) {
            case Col c -> add(out, c.name(), allowed(List.of(Boolean.TRUE), source));
            case IsBool b -> {
                String col = SqlExpr.columnName(b.expr());
                if (col == null) return;
                add(out, col, b.negated() ? excluded(List.of(b.value())) : allowed(List.of(b.value()), source));
            }
            case Bin b when b.op() == Op.OR -> extractOr(b, source, out, k);
            case Bin b when b.op().isComparison() -> {
                String col = SqlExpr.columnName(b.left());
                SqlExpr other = b.right();
                Op op = b.op();
                if (col == null) {
                    col = SqlExpr.columnName(b.right());
                    other = b.left();
                    op = op.flip();
                }
                if (col != null && SqlExpr.columnsOf(other).isEmpty()) {
                    Optional<Object> v = k.value(other);
                    if (v.isPresent()) add(out, col, bound(op, v.get(), source));
                    return;
                }
                if (b.op() == Op.EQ && shapeHint(b.left(), b.right(), out, k)) return;
                if (b.op() == Op.EQ && shapeHint(b.right(), b.left(), out, k)) return;
                String lenCol = lengthColumn(b.left(), k);
                SqlExpr lenOther = b.right();
                Op lop = b.op();
                if (lenCol == null) {
                    lenCol = lengthColumn(b.right(), k);
                    lenOther = b.left();
                    lop = lop.flip();
                }
                BigDecimal n = lenCol == null ? null : number(lenOther, k);
                if (n != null) {
                    int v = n.intValue();
                    switch (lop) {
                        case GT -> add(out, lenCol, rule(d -> d.minLen = v + 1));
                        case GE -> add(out, lenCol, rule(d -> d.minLen = v));
                        case LT -> add(out, lenCol, rule(d -> d.maxLen = v - 1));
                        case LE -> add(out, lenCol, rule(d -> d.maxLen = v));
                        case EQ -> add(out, lenCol, rule(d -> { d.minLen = v; d.maxLen = v; }));
                        default -> {}
                    }
                }
            }
            case AnyAll a -> {
                String col = SqlExpr.columnName(a.left());
                Object arr = col == null ? null : k.value(a.array()).orElse(null);
                if (!(arr instanceof List<?> list)) return;
                if (a.op() == Op.EQ && !a.all()) add(out, col, allowed(list, source));
                else if (a.op() == Op.NE && a.all()) add(out, col, excluded(list));
                else if (a.op() == Op.LIKE && !a.all() && list.size() == 1) add(out, col, like(String.valueOf(list.get(0))));
            }
            case InList i -> {
                String col = SqlExpr.columnName(i.expr());
                if (col == null) return;
                List<Object> vals = new ArrayList<>();
                for (SqlExpr v : i.values()) {
                    Optional<Object> c = k.value(v);
                    if (c.isEmpty()) return;
                    vals.add(c.get());
                }
                add(out, col, i.negated() ? excluded(vals) : allowed(vals, source));
            }
            case Between b -> {
                String col = SqlExpr.columnName(b.expr());
                Optional<Object> lo = k.value(b.low());
                Optional<Object> hi = k.value(b.high());
                if (col == null || b.negated() || lo.isEmpty() || hi.isEmpty()) return;
                add(out, col, bound(Op.GE, lo.get(), source).and(bound(Op.LE, hi.get(), source)));
            }
            case IsNull n when n.negated() -> {
                String col = SqlExpr.columnName(n.expr());
                if (col != null) add(out, col, rule(d -> d.notNull = true));
            }
            // a value matching the pattern as written also matches it case-insensitively (~~*, ~*)
            case Bin b when b.op() == Op.LIKE || b.op() == Op.ILIKE || b.op() == Op.REGEX || b.op() == Op.IREGEX -> {
                String col = SqlExpr.columnName(b.left());
                Object p = col == null ? null : k.value(b.right()).orElse(null);
                if (!(p instanceof String s)) return;
                add(out, col, b.op() == Op.LIKE || b.op() == Op.ILIKE ? like(s) : rule(d -> d.regexes.add(s)));
            }
            case Case c -> {
                String col = c.operand() == null ? null : SqlExpr.columnName(c.operand());
                if (col == null || !(c.otherwise() instanceof Lit l) || !Boolean.FALSE.equals(l.value())) return;
                List<Object> vals = new ArrayList<>();
                for (SqlExpr w : c.whens()) {
                    Optional<Object> v = k.value(w);
                    if (v.isEmpty()) return;
                    vals.add(v.get());
                }
                add(out, col, allowed(vals, source));
            }
            default -> {}
        }
    }

    private static void extractOr(Bin or, String source, Map<String, ColumnRules> out, Constants k) {
        List<SqlExpr> rest = new ArrayList<>();
        Set<String> nullable = new LinkedHashSet<>();
        for (SqlExpr d : disjuncts(or)) {
            if (d instanceof IsNull n && !n.negated() && SqlExpr.columnName(n.expr()) != null) {
                nullable.add(SqlExpr.columnName(n.expr()));
                continue;
            }
            rest.add(d);
        }
        if (rest.size() == 1) {
            Map<String, ColumnRules> facts = extract(rest.get(0), source, k);
            boolean nullGuarded = !nullable.isEmpty() && nullable.containsAll(facts.keySet());
            if (nullGuarded) facts.forEach((col, r) -> add(out, col, r));
            return;
        }
        String col = null;
        List<Object> vals = new ArrayList<>();
        for (SqlExpr d : rest) {
            if (!(d instanceof Bin || d instanceof InList || d instanceof AnyAll)) return;
            Map<String, ColumnRules> one = new HashMap<>();
            extractOne(d, source, one, k);
            if (one.size() != 1) return;
            Map.Entry<String, ColumnRules> e = one.entrySet().iterator().next();
            if (col != null && !col.equals(e.getKey())) return;
            col = e.getKey();
            ColumnRules r = e.getValue();
            if (r.allowed == null || r.lo != null || r.hi != null || !r.excluded.isEmpty()) return;
            vals.addAll(r.allowed);
        }
        if (col != null) add(out, col, allowed(vals, source));
    }

    private static boolean shapeHint(SqlExpr side, SqlExpr other, Map<String, ColumnRules> out, Constants k) {
        String col = SqlExpr.columnName(side);
        SqlExpr o = SqlExpr.unwrap(other);
        if (col != null && o instanceof Func f && f.args().size() == 1 && (f.name().equals("lower") || f.name().equals("upper"))
                && col.equals(SqlExpr.columnName(stripTrim(f.args().get(0))))) {
            LetterCase letterCase = f.name().equals("lower") ? LetterCase.LOWER : LetterCase.UPPER;
            add(out, col, rule(d -> d.letterCase = letterCase));
            return true;
        }
        if (col != null && o instanceof Func f && f.name().equals("date_trunc") && f.args().size() == 2
                && k.value(f.args().get(0)).orElse(null) instanceof String u && col.equals(SqlExpr.columnName(f.args().get(1)))
                && TruncUnit.of(u).isPresent()) {
            TruncUnit unit = TruncUnit.of(u).get();
            add(out, col, rule(d -> d.truncUnit = unit));
            return true;
        }
        SqlExpr s = SqlExpr.unwrap(side);
        if (s instanceof Func f && f.args().size() == 1 && (f.name().equals("jsonb_typeof") || f.name().equals("json_typeof"))) {
            String c = SqlExpr.columnName(f.args().get(0));
            if (c != null && k.value(other).orElse(null) instanceof String kind && JsonKind.of(kind).isPresent()) {
                JsonKind jsonKind = JsonKind.of(kind).get();
                add(out, c, rule(d -> d.jsonKind = jsonKind));
                return true;
            }
        }
        if (s instanceof Func f && (f.name().equals("substring") || f.name().equals("substr") || f.name().equals("left"))
                && f.args().size() >= 2 && k.value(other).isPresent()
                && f.args().subList(1, f.args().size()).stream().allMatch(a -> k.value(a).isPresent())) {
            String c = SqlExpr.columnName(f.args().get(0));
            Object lit = k.value(other).get();
            BigDecimal a1 = number(f.args().get(1), k);
            BigDecimal a2 = f.args().size() > 2 ? number(f.args().get(2), k) : null;
            int from = f.name().equals("left") ? 1 : a1 == null ? 0 : a1.intValue();
            int len = f.name().equals("left") ? (a1 == null ? -1 : a1.intValue()) : a2 == null ? -1 : a2.intValue();
            if (c != null && lit instanceof String l && from >= 1 && (len < 0 || len == l.length())) {
                boolean fixesWholeTail = len < 0 && !f.name().equals("left");
                String rest = fixesWholeTail ? "" : "%";
                add(out, c, like("_".repeat(from - 1) + l.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + rest));
                return true;
            }
        }
        if (s instanceof Func f && f.name().equals("date_part") && f.args().size() == 2) {
            String c = SqlExpr.columnName(f.args().get(1));
            Object field = k.value(f.args().get(0)).orElse(null);
            BigDecimal v = number(other, k);
            if (c != null && "day".equals(field) && v != null && v.compareTo(BigDecimal.ONE) == 0) {
                add(out, c, rule(d -> d.truncUnit = TruncUnit.MONTH));
                return true;
            }
        }
        return false;
    }

    private static SqlExpr stripTrim(SqlExpr e) {
        SqlExpr u = SqlExpr.unwrap(e);
        if (u instanceof Func t && (t.name().equals("btrim") || t.name().equals("trim")) && t.args().size() == 1) return t.args().get(0);
        return e;
    }

    private static ColumnRules excluded(Collection<?> values) {
        return rule(d -> d.excluded.addAll(values));
    }

    private static ColumnRules like(String pattern) {
        return rule(d -> d.likes.add(pattern));
    }

    public static ColumnRules bound(Op op, Object v, String source) {
        return switch (op) {
            case EQ -> allowed(List.of(v), source);
            case NE -> excluded(List.of(v));
            case GT -> rule(d -> { d.lo = v; d.loInclusive = false; });
            case GE -> rule(d -> d.lo = v);
            case LT -> rule(d -> { d.hi = v; d.hiInclusive = false; });
            case LE -> rule(d -> d.hi = v);
            default -> throw new IllegalArgumentException(op.sql + " is not a comparison");
        };
    }

    private static String lengthColumn(SqlExpr e, Constants k) {
        SqlExpr u = SqlExpr.unwrap(e);
        if (!(u instanceof Func f) || f.args().isEmpty()) return null;
        switch (f.name()) {
            case "length", "char_length", "character_length", "octet_length" -> {
                if (f.args().size() != 1) return null;
                return SqlExpr.columnName(stripTrim(f.args().get(0)));
            }
            case "cardinality" -> {
                return f.args().size() == 1 ? SqlExpr.columnName(f.args().get(0)) : null;
            }
            case "array_length" -> {
                BigDecimal dim = f.args().size() == 2 ? number(f.args().get(1), k) : null;
                return dim != null && dim.intValue() == 1 ? SqlExpr.columnName(f.args().get(0)) : null;
            }
            default -> {
                return null;
            }
        }
    }

    private static BigDecimal number(SqlExpr e, Constants k) {
        return SqlValues.num(k.value(e).orElse(null));
    }

    private static Integer max(Integer a, int b) { return a == null ? b : Math.max(a, b); }
    private static Integer min(Integer a, int b) { return a == null ? b : Math.min(a, b); }
}
