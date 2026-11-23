package io.github.rowsmith.internal.sql;

import io.github.rowsmith.internal.sql.SqlExpr.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SqlValues {

    private SqlValues() {}

    public static Optional<Object> literal(SqlExpr e) {
        return switch (e) {
            case Lit l -> Optional.ofNullable(l.value());
            case Unary u when u.op() == UnaryOp.NEG && u.expr() instanceof Lit l && l.value() instanceof BigDecimal n -> Optional.of(n.negate());
            default -> Optional.empty();
        };
    }

    public static Boolean equal(Object l, Object r) {
        if (l instanceof List<?> a && r instanceof List<?> b) {
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++) {
                Boolean e = equal(a.get(i), b.get(i));
                if (!Boolean.TRUE.equals(e)) return e;
            }
            return true;
        }
        if (l instanceof Boolean || r instanceof Boolean) {
            Boolean bl = bool(l);
            Boolean br = bool(r);
            return bl == null || br == null ? null : bl.equals(br);
        }
        Integer c = compare(l, r);
        if (c != null) return c == 0;
        return String.valueOf(l).equals(String.valueOf(r));
    }

    public static Integer compare(Object l, Object r) {
        BigDecimal nl = l instanceof Number || r instanceof Number ? num(l) : null;
        BigDecimal nr = nl != null ? num(r) : null;
        if (nl != null && nr != null) return nl.compareTo(nr);
        if (l instanceof Temporal || r instanceof Temporal) return compareTemporal(l, r);
        if (l instanceof String a && r instanceof String b) return a.compareTo(b);
        if (l instanceof Boolean || r instanceof Boolean) {
            Boolean a = bool(l);
            Boolean b = bool(r);
            return a == null || b == null ? null : a.compareTo(b);
        }
        return null;
    }

    private static Integer compareTemporal(Object l, Object r) {
        return switch (l) {
            case OffsetDateTime a when r instanceof OffsetDateTime b -> OffsetDateTime.timeLineOrder().compare(a, b);
            case LocalDateTime a when r instanceof LocalDateTime b -> a.compareTo(b);
            case LocalDateTime a when r instanceof LocalDate b -> a.compareTo(b.atStartOfDay());
            case LocalDate a when r instanceof LocalDate b -> a.compareTo(b);
            case LocalDate a when r instanceof LocalDateTime b -> a.atStartOfDay().compareTo(b);
            case LocalTime a when r instanceof LocalTime b -> a.compareTo(b);
            default -> null;
        };
    }

    private static Boolean bool(Object v) {
        return v instanceof Boolean b ? b : null;
    }

    public static BigDecimal num(Object v) {
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
            return BigDecimal.valueOf(((Number) v).longValue());
        }
        if (v instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return new BigDecimal(String.valueOf(d));
        }
        if (v instanceof String s) {
            try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    public static boolean like(String s, String pattern, boolean ci) {
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\' && i + 1 < pattern.length()) {
                re.append(Pattern.quote(String.valueOf(pattern.charAt(++i))));
            } else if (c == '%') re.append(".*");
            else if (c == '_') re.append('.');
            else re.append(Pattern.quote(String.valueOf(c)));
        }
        int flags = Pattern.DOTALL | (ci ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        return Pattern.compile(re.toString(), flags).matcher(s).matches();
    }
}
