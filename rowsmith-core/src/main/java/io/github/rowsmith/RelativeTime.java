package io.github.rowsmith;

import io.github.rowsmith.internal.config.DateExpr;
import java.time.Duration;
import java.time.Period;

/**
 * A date or time relative to Rowsmith's clock (the application's clock, or a fixed one in tests), for date and
 * timestamp columns: {@code set(TASK.DUE_DATE, today().minusDays(3))}, {@code now().minusHours(2)}.
 * Immutable.
 */
public final class RelativeTime {

    private final DateExpr expr;

    private RelativeTime(DateExpr expr) {
        this.expr = expr;
    }

    /** The start of today. */
    public static RelativeTime today() {
        return new RelativeTime(DateExpr.of(false, Period.ZERO, Duration.ZERO));
    }

    /** The current instant. */
    public static RelativeTime now() {
        return new RelativeTime(DateExpr.of(true, Period.ZERO, Duration.ZERO));
    }

    /** Parses the text form used in string-based specs, e.g. {@code "today-3d"}, {@code "now+2h"}. */
    public static RelativeTime parse(String text) {
        DateExpr e = DateExpr.parse(text);
        if (e == null) {
            throw new RowsmithException(ErrorCode.INVALID_VALUE, "'" + text + "' is not a relative time like today-3d or now+2h; units: "
                    + String.join(", ", DateExpr.units()));
        }
        return new RelativeTime(e);
    }

    public RelativeTime plus(Period p) {
        return new RelativeTime(DateExpr.of(expr.now, expr.period.plus(p), expr.duration));
    }

    public RelativeTime minus(Period p) {
        return plus(p.negated());
    }

    public RelativeTime plus(Duration d) {
        return new RelativeTime(DateExpr.of(expr.now, expr.period, expr.duration.plus(d)));
    }

    public RelativeTime minus(Duration d) {
        return plus(d.negated());
    }

    public RelativeTime plusDays(int days) { return plus(Period.ofDays(days)); }
    public RelativeTime minusDays(int days) { return minus(Period.ofDays(days)); }
    public RelativeTime plusMonths(int months) { return plus(Period.ofMonths(months)); }
    public RelativeTime minusMonths(int months) { return minus(Period.ofMonths(months)); }
    public RelativeTime plusHours(long hours) { return plus(Duration.ofHours(hours)); }
    public RelativeTime minusHours(long hours) { return minus(Duration.ofHours(hours)); }

    DateExpr expr() {
        return expr;
    }

    @Override
    public String toString() {
        return (expr.now ? "now" : "today") + (expr.period.isZero() ? "" : " " + expr.period) + (expr.duration.isZero() ? "" : " " + expr.duration);
    }
}
