package rowsmith.internal.gen;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import rowsmith.ErrorCode;
import rowsmith.PgEnum;
import rowsmith.RowsmithException;
import rowsmith.internal.config.DateExpr;
import rowsmith.internal.config.RowsmithClock;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.PgType;

public final class Coercer {

    private Coercer() {}

    public static Object coerce(ColumnDef col, Object v, RowsmithClock clock) {
        return coerce(col.type, v, clock, col.qualifiedName());
    }

    private static Object coerce(PgType type, Object v, RowsmithClock clock, String where) {
        if (v == null) return null;
        TypeKind kind = TypeKind.of(type);
        PgType base = TypeKind.strip(type);
        Object value = v instanceof PgEnum e ? e.label() : v;
        return switch (kind) {
            case TEXT, CHAR -> value instanceof BigDecimal b ? b.toPlainString() : value.toString();
            case ENUM -> {
                String s = value.toString();
                if (!base.enumLabels.contains(s)) {
                    throw new RowsmithException(ErrorCode.NOT_IN_ENUM, where + " = \"" + s + "\" is not a value of enum "
                            + base.castName + "; allowed: " + String.join(", ", base.enumLabels));
                }
                yield s;
            }
            case DATE, TIMESTAMP, TIMESTAMPTZ -> temporal(kind, value, clock);
            case JSON -> {
                if (value instanceof String s) yield s;
                throw invalid(where, value, base.castName + " (pass JSON text)");
            }
            case ARRAY -> {
                if (value instanceof String s) yield s;
                Collection<?> c = value instanceof Collection<?> cc ? cc : value instanceof Object[] arr ? List.of(arr) : null;
                if (c == null) throw invalid(where, value, base.castName + " (pass a List)");
                List<Object> out = new ArrayList<>();
                for (Object e : c) out.add(coerce(base.element(), e, clock, where + "[]"));
                yield out;
            }
            default -> value;
        };
    }

    private static Object temporal(TypeKind kind, Object v, RowsmithClock clock) {
        if (kind == TypeKind.TIMESTAMPTZ && v instanceof OffsetDateTime o) return o;
        LocalDateTime local = switch (v) {
            case LocalDateTime t -> t;
            case LocalDate d -> d.atStartOfDay();
            case java.sql.Date d -> d.toLocalDate().atStartOfDay();
            case OffsetDateTime o -> LocalDateTime.ofInstant(o.toInstant(), clock.zone());
            case ZonedDateTime z -> LocalDateTime.ofInstant(z.toInstant(), clock.zone());
            case Instant i -> LocalDateTime.ofInstant(i, clock.zone());
            case java.util.Date d -> LocalDateTime.ofInstant(d.toInstant(), clock.zone());
            case DateExpr de -> de.toDateTime(clock);
            case String s when DateExpr.parse(s) != null -> DateExpr.parse(s).toDateTime(clock);
            default -> null;
        };
        if (local == null) return v;
        return switch (kind) {
            case DATE -> local.toLocalDate();
            case TIMESTAMP -> local;
            default -> clock.toOffset(local);
        };
    }

    private static RowsmithException invalid(String where, Object v, String type) {
        return new RowsmithException(ErrorCode.INVALID_VALUE, where + " = " + (v instanceof String ? "\"" + v + "\"" : v)
                + " is not a valid " + type);
    }
}
