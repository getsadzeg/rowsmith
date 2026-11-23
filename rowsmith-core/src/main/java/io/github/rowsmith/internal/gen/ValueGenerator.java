package io.github.rowsmith.internal.gen;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.internal.config.RowsmithClock;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.PgType;
import io.github.rowsmith.internal.schema.Typmod;
import io.github.rowsmith.internal.sql.ColumnRules;
import io.github.rowsmith.internal.sql.SqlValues;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public final class ValueGenerator {

    public enum Uniqueness {
        NONE,
        PART_OF_KEY,
        ALONE
    }

    public record Request(ColumnRules rules, long number, Uniqueness uniqueness) {}
    private static final int DEFAULT_SCALE = 2;
    private static final String HINT = "; set it in the test or with RowsmithSetup.value(...)";
    private final RowsmithClock clock;
    public ValueGenerator(RowsmithClock clock) {
        this.clock = clock;
    }

    public Object generate(ColumnDef col, Request req) {
        ColumnRules rules = req.rules() == null ? new ColumnRules() : req.rules();
        return value(col, col.type, col.effectiveTypmod(), rules, req.number(), req, true);
    }

    private Object value(ColumnDef col, PgType type, int typmod, ColumnRules rules, long n, Request req, boolean top) {
        TypeKind kind = TypeKind.of(type);
        PgType base = TypeKind.strip(type);
        List<Object> allowed = top && rules.allowed != null ? new ArrayList<>(rules.allowed) : null;
        if (kind == TypeKind.ENUM) allowed = intersect(allowed, new ArrayList<>(base.enumLabels));
        if (kind == TypeKind.BOOL) allowed = intersect(allowed, List.of(false, true));
        if (allowed != null) return fromList(col, allowed, rules, kind, top ? typmod : -1, n, top ? req.uniqueness() : Uniqueness.NONE);

        return switch (kind) {
            case INT2, INT4, INT8, NUMERIC, FLOAT, MONEY -> number(col, kind, typmod, rules, n, req);
            case UUID -> new UUID(0, n);
            case ENUM, BOOL -> throw new IllegalStateException("handled as allowed values");
            default -> throw new RowsmithException(ErrorCode.UNSUPPORTED_TYPE, "cannot generate a value of type " + type.castName
                    + " for column " + col.qualifiedName() + HINT + ", or give the column a DEFAULT");
        };
    }

    private static Object fromList(ColumnDef col, List<Object> allowed, ColumnRules rules, TypeKind kind, int typmod, long n, Uniqueness u) {
        allowed.removeIf(v -> v == null || contains(rules.excluded, v) || !fits(v, rules, kind, typmod));
        if (allowed.isEmpty()) {
            throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "no value satisfies all constraints on column " + col.qualifiedName()
                    + " (allowed lists and exclusions do not overlap)" + HINT);
        }
        long index = switch (u) {
            case NONE -> 0;
            case PART_OF_KEY -> (n - 1) % allowed.size();
            case ALONE -> n - 1;
        };
        if (index >= allowed.size()) {
            throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "column " + col.qualifiedName() + " is unique but allows only "
                    + allowed.size() + " values " + allowed + ", so row " + n + " has none left" + HINT);
        }
        Object v = allowed.get((int) index);
        return kind == TypeKind.ENUM || kind == TypeKind.TEXT || kind == TypeKind.CHAR ? String.valueOf(v) : v;
    }

    private static List<Object> intersect(List<Object> a, List<Object> b) {
        if (a == null) return new ArrayList<>(b);
        List<Object> out = new ArrayList<>();
        for (Object x : a) {
            for (Object y : b) {
                Integer c = SqlValues.compare(x, y);
                if (c != null ? c == 0 : String.valueOf(x).equals(String.valueOf(y))) {
                    out.add(y);
                    break;
                }
            }
        }
        return out;
    }

    private static boolean fits(Object v, ColumnRules rules, TypeKind kind, int typmod) {
        if (v instanceof Boolean && kind != TypeKind.BOOL) return false;
        if (kind == TypeKind.TEXT || kind == TypeKind.CHAR) {
            String s = String.valueOf(v);
            int len = s.codePointCount(0, s.length());
            if (Typmod.isSet(typmod) && len > Typmod.length(typmod)) return false;
            if (rules.maxLen != null && len > rules.maxLen) return false;
            if (rules.minLen != null && len < rules.minLen && kind == TypeKind.TEXT) return false;
            for (String p : rules.likes) if (!SqlValues.like(String.valueOf(v), p, false)) return false;
        }
        if (rules.lo != null) {
            Integer c = SqlValues.compare(v, rules.lo);
            if (c != null && (c < 0 || (c == 0 && !rules.loInclusive))) return false;
        }
        if (rules.hi != null) {
            Integer c = SqlValues.compare(v, rules.hi);
            if (c != null && (c > 0 || (c == 0 && !rules.hiInclusive))) return false;
        }
        return true;
    }

    private static boolean contains(Set<Object> set, Object v) {
        for (Object x : set) {
            Integer c = SqlValues.compare(v, x);
            if (c != null ? c == 0 : v.equals(x)) return true;
        }
        return false;
    }

    private static Object number(ColumnDef col, TypeKind kind, int typmod, ColumnRules rules, long n, Request req) {
        int scale;
        BigDecimal maxAbs;
        switch (kind) {
            case INT2 -> { scale = 0; maxAbs = BigDecimal.valueOf(Short.MAX_VALUE); }
            case INT4 -> { scale = 0; maxAbs = BigDecimal.valueOf(Integer.MAX_VALUE); }
            case INT8 -> { scale = 0; maxAbs = BigDecimal.valueOf(Long.MAX_VALUE); }
            case NUMERIC -> {
                if (Typmod.isSet(typmod)) {
                    int p = Typmod.precision(typmod);
                    scale = Typmod.scale(typmod);
                    maxAbs = BigDecimal.ONE.scaleByPowerOfTen(p - scale).subtract(BigDecimal.ONE.movePointLeft(scale));
                } else {
                    scale = DEFAULT_SCALE;
                    maxAbs = null;
                }
            }
            default -> { scale = DEFAULT_SCALE; maxAbs = null; }
        }
        BigDecimal ulp = BigDecimal.ONE.movePointLeft(scale);
        BigDecimal lo = bound(rules.lo, rules.loInclusive, ulp, true);
        BigDecimal hi = bound(rules.hi, rules.hiInclusive, ulp, false);
        if (maxAbs != null) {
            lo = max(lo, maxAbs.negate());
            hi = min(hi, maxAbs);
        }
        if (lo != null) lo = lo.setScale(scale, RoundingMode.CEILING);
        if (hi != null) hi = hi.setScale(scale, RoundingMode.FLOOR);
        if (lo != null && hi != null && lo.compareTo(hi) > 0) {
            throw noValueFits(kind, col, lo.toPlainString() + " .. " + hi.toPlainString());
        }
        BigDecimal unit = ulp.max(BigDecimal.ONE);
        BigDecimal step = scale == 0 || lo == null || hi == null || hi.subtract(lo).compareTo(unit) >= 0 ? unit : ulp;
        boolean down = hi != null && hi.compareTo(unit) < 0 && (lo == null || lo.compareTo(unit) < 0);
        BigDecimal start = down ? hi : lo != null && lo.compareTo(unit) > 0 ? lo : unit;
        BigDecimal end = down ? lo : hi;
        BigInteger index = BigInteger.valueOf(n - 1);
        List<BigDecimal> skipped = sortedExcluded(rules, down).stream()
                .filter(x -> x.subtract(start).remainder(step).signum() == 0 && !beyond(start, x, down) && (end == null || !beyond(x, end, down)))
                .toList();
        if (end != null) {
            BigInteger count = end.subtract(start).abs().divide(step, 0, RoundingMode.FLOOR).toBigInteger().add(BigInteger.ONE)
                    .subtract(BigInteger.valueOf(skipped.size()));
            if (count.signum() == 0) {
                throw noValueFits(kind, col, start.toPlainString() + " .. " + end.toPlainString() + " without " + skipped);
            }
            if (index.compareTo(count) >= 0) {
                if (req.uniqueness() == Uniqueness.ALONE) {
                    throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "column " + col.qualifiedName() + " is unique but its constraints allow only "
                            + count + " values from " + start.toPlainString() + ", so row " + n + " has none left" + HINT);
                }
                index = index.mod(count);
            }
        }
        BigDecimal dir = down ? step.negate() : step;
        BigDecimal v = start.add(dir.multiply(new BigDecimal(index)));
        for (BigDecimal x : skipped) if (!beyond(x, v, down)) v = v.add(dir);
        if (scale == 0 && kind != TypeKind.NUMERIC) return v.longValueExact();
        return v.setScale(scale, RoundingMode.UNNECESSARY);
    }

    private static RowsmithException noValueFits(TypeKind kind, ColumnDef col, String range) {
        return new RowsmithException(ErrorCode.CANNOT_GENERATE, "no " + kind.name().toLowerCase(Locale.ROOT) + " value fits the constraints on column "
                + col.qualifiedName() + " (range " + range + ")" + HINT);
    }

    private static boolean beyond(BigDecimal x, BigDecimal limit, boolean down) {
        return down ? x.compareTo(limit) < 0 : x.compareTo(limit) > 0;
    }

    private static List<BigDecimal> sortedExcluded(ColumnRules rules, boolean down) {
        SortedSet<BigDecimal> out = new TreeSet<>(down ? Comparator.<BigDecimal>reverseOrder() : Comparator.<BigDecimal>naturalOrder());
        for (Object x : rules.excluded) {
            BigDecimal b = SqlValues.num(x);
            if (b != null) out.add(b);
        }
        return List.copyOf(out);
    }

    private static BigDecimal bound(Object v, boolean inclusive, BigDecimal ulp, boolean lower) {
        BigDecimal n = v == null ? null : SqlValues.num(v);
        if (n == null) return null;
        if (!inclusive) n = lower ? n.add(ulp) : n.subtract(ulp);
        return n;
    }

    private static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a == null ? b : b == null ? a : a.max(b);
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a == null ? b : b == null ? a : a.min(b);
    }
}
