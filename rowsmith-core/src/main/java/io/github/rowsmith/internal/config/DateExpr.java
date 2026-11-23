package io.github.rowsmith.internal.config;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateExpr {

    private static final Pattern WHOLE = Pattern.compile("^\\s*(today|now)((\\s*[+-]\\s*\\d+\\s*(min|s|h|d|w|m|y))*)\\s*$");
    private static final Pattern PART = Pattern.compile("([+-])\\s*(\\d+)\\s*(min|s|h|d|w|m|y)");

    public final boolean now;
    public final Period period;
    public final Duration duration;

    private DateExpr(boolean now, Period period, Duration duration) {
        this.now = now;
        this.period = period;
        this.duration = duration;
    }

    public static DateExpr of(boolean now, Period period, Duration duration) {
        return new DateExpr(now, period, duration);
    }

    public static DateExpr parse(String s) {
        Matcher m = WHOLE.matcher(s);
        if (!m.matches()) return null;
        Period p = Period.ZERO;
        Duration d = Duration.ZERO;
        Matcher pm = PART.matcher(m.group(2));
        while (pm.find()) {
            int n = Integer.parseInt(pm.group(2)) * (pm.group(1).equals("-") ? -1 : 1);
            switch (pm.group(3)) {
                case "s" -> d = d.plusSeconds(n);
                case "min" -> d = d.plusMinutes(n);
                case "h" -> d = d.plusHours(n);
                case "d" -> p = p.plusDays(n);
                case "w" -> p = p.plusDays(7L * n);
                case "m" -> p = p.plusMonths(n);
                case "y" -> p = p.plusYears(n);
                default -> throw new IllegalStateException();
            }
        }
        return new DateExpr(m.group(1).equals("now"), p, d);
    }

    public LocalDateTime toDateTime(RowsmithClock clock) {
        LocalDateTime base = now ? clock.now() : clock.today().atStartOfDay();
        return base.plus(period).plus(duration);
    }

    public static List<String> units() {
        return new ArrayList<>(List.of("s", "min", "h", "d", "w", "m (months)", "y"));
    }
}
