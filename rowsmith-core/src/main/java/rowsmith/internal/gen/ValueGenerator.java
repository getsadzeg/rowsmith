package rowsmith.internal.gen;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.config.RgxGenOption;
import com.github.curiousoddman.rgxgen.config.RgxGenProperties;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rowsmith.ErrorCode;
import rowsmith.RowsmithException;
import rowsmith.internal.config.RowsmithClock;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.PgType;
import rowsmith.internal.schema.Typmod;
import rowsmith.internal.sql.ColumnRules;
import rowsmith.internal.sql.ColumnRules.JsonKind;
import rowsmith.internal.sql.ColumnRules.TruncUnit;
import rowsmith.internal.sql.SqlValues;

public final class ValueGenerator {

    public enum Uniqueness {
        NONE,
        PART_OF_KEY,
        ALONE
    }

    public record Request(ColumnRules rules, long number, Uniqueness uniqueness) {}

    private static final int MAX_UNBOUNDED_REPEAT = 3;
    private static final int DEFAULT_SCALE = 2;
    private static final int DEFAULT_BYTES = 8;
    private static final Character ANY_CHAR = null;
    private static final String HINT = "; set it in the test or with RowsmithSetup.value(...)";
    private static final Pattern POSIX_CLASS = Pattern.compile("\\[:([^:\\]]*):\\]");
    private static final Map<String, String> POSIX_CLASSES = Map.ofEntries(
            Map.entry("alpha", "A-Za-z"),
            Map.entry("digit", "0-9"),
            Map.entry("alnum", "A-Za-z0-9"),
            Map.entry("upper", "A-Z"),
            Map.entry("lower", "a-z"),
            Map.entry("xdigit", "0-9A-Fa-f"),
            Map.entry("word", "A-Za-z0-9_"),
            Map.entry("blank", " \t"),
            Map.entry("space", " \t\n\r\f\u000B"),
            Map.entry("punct", "!-/:-@\\[-`{-~"),
            Map.entry("graph", "!-~"),
            Map.entry("print", " -~"));

    private final RowsmithClock clock;
    private final RgxGenProperties regexProperties = new RgxGenProperties();

    public ValueGenerator(RowsmithClock clock) {
        this.clock = clock;
        RgxGenOption.INFINITE_PATTERN_REPETITION.setInProperties(regexProperties, MAX_UNBOUNDED_REPEAT);
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
            case TEXT, CHAR -> text(col, kind, typmod, rules, n, top ? req.uniqueness() : Uniqueness.NONE);
            case UUID -> new UUID(0, n);
            case DATE, TIMESTAMP, TIMESTAMPTZ -> temporal(col, kind, rules, n, req);
            case TIME, TIMETZ -> time(col, n, req);
            case INTERVAL -> n + " days";
            case JSON -> json(rules.jsonKind, n);
            case BYTEA -> bytes(n, length(rules, DEFAULT_BYTES));
            case INET -> "10." + ((n >> 16) & 0xff) + "." + ((n >> 8) & 0xff) + "." + (n & 0xff);
            case CIDR -> "10." + ((n >> 8) & 0xff) + "." + (n & 0xff) + ".0/24";
            case MACADDR -> "02:00:00:" + HexFormat.ofDelimiter(":").formatHex(bytes(n, 3));
            case TSVECTOR -> col.name + "-" + n;
            case XML -> "<value>" + n + "</value>";
            case VECTOR -> vector(typmod, n);
            case BIT -> bits(base, typmod, n);
            case ARRAY -> array(col, base, rules, n, req, top);
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

    private record Step(long amount, ChronoUnit unit) {
        static Step of(TruncUnit truncUnit, boolean isDate) {
            if (truncUnit == null) return new Step(1, ChronoUnit.DAYS);
            return switch (truncUnit) {
                case YEAR -> new Step(1, ChronoUnit.YEARS);
                case QUARTER -> new Step(3, ChronoUnit.MONTHS);
                case MONTH -> new Step(1, ChronoUnit.MONTHS);
                case WEEK -> new Step(1, ChronoUnit.WEEKS);
                case DAY -> new Step(1, ChronoUnit.DAYS);
                case HOUR -> new Step(1, isDate ? ChronoUnit.DAYS : ChronoUnit.HOURS);
                case MINUTE -> new Step(1, isDate ? ChronoUnit.DAYS : ChronoUnit.MINUTES);
                case SECOND -> new Step(1, isDate ? ChronoUnit.DAYS : ChronoUnit.SECONDS);
            };
        }

        LocalDateTime plus(LocalDateTime t, long times) {
            return t.plus(amount * times, unit);
        }

        LocalDateTime ceil(LocalDateTime t, TruncUnit truncUnit) {
            if (truncUnit == null) return t;
            LocalDateTime tr = truncate(t, truncUnit);
            return tr.isBefore(t) ? truncate(plus(tr, 1), truncUnit) : tr;
        }

        LocalDateTime floor(LocalDateTime t, TruncUnit truncUnit) {
            return truncUnit == null ? t : truncate(t, truncUnit);
        }
    }

    private Object temporal(ColumnDef col, TypeKind kind, ColumnRules rules, long n, Request req) {
        boolean isDate = kind == TypeKind.DATE;
        long unitSeconds = isDate ? ChronoUnit.DAYS.getDuration().getSeconds() : 1;
        LocalDateTime lo = dt(rules.lo, rules.loInclusive, unitSeconds, true);
        LocalDateTime hi = dt(rules.hi, rules.hiInclusive, unitSeconds, false);
        if (isDate) {
            if (lo != null) lo = lo.toLocalDate().atStartOfDay().isBefore(lo) ? lo.toLocalDate().plusDays(1).atStartOfDay() : lo;
            if (hi != null) hi = hi.toLocalDate().atStartOfDay();
        }
        Step step = Step.of(rules.truncUnit, isDate);
        if (lo != null) lo = step.ceil(lo, rules.truncUnit);
        if (hi != null) hi = step.floor(hi, rules.truncUnit);
        if (lo != null && hi != null && lo.isAfter(hi)) {
            throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "no date/time fits the constraints on column " + col.qualifiedName() + HINT);
        }
        LocalDateTime first = step.ceil(step.plus(clock.today().atStartOfDay(), 1), rules.truncUnit);
        boolean down = hi != null && hi.isBefore(first) && (lo == null || lo.isBefore(first));
        LocalDateTime start = down ? hi : lo != null && lo.isAfter(first) ? lo : first;
        LocalDateTime end = down ? lo : hi;
        long index = n - 1;
        if (end != null) {
            long count = Math.abs(step.unit().between(start, end)) / step.amount() + 1;
            if (index >= count) {
                if (req.uniqueness() == Uniqueness.ALONE) {
                    throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "column " + col.qualifiedName() + " is unique but its constraints allow only "
                            + count + " values, so row " + n + " has none left" + HINT);
                }
                index %= count;
            }
        }
        LocalDateTime t = step.plus(start, down ? -index : index);
        if (isDate) return t.toLocalDate();
        if (kind == TypeKind.TIMESTAMPTZ) return clock.toOffset(t);
        return t;
    }

    private static LocalTime time(ColumnDef col, long n, Request req) {
        long count = ChronoUnit.DAYS.getDuration().toSeconds();
        if (n > count && req.uniqueness() == Uniqueness.ALONE) {
            throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "column " + col.qualifiedName() + " is unique but a day has only "
                    + count + " seconds, so row " + n + " has no time left" + HINT);
        }
        return LocalTime.ofSecondOfDay(n % count);
    }

    private static LocalDateTime truncate(LocalDateTime t, TruncUnit unit) {
        return switch (unit) {
            case YEAR -> t.toLocalDate().withDayOfYear(1).atStartOfDay();
            case QUARTER -> t.toLocalDate().withMonth((t.getMonthValue() - 1) / 3 * 3 + 1).withDayOfMonth(1).atStartOfDay();
            case MONTH -> t.toLocalDate().withDayOfMonth(1).atStartOfDay();
            case WEEK -> t.toLocalDate().minusDays(t.getDayOfWeek().getValue() - 1L).atStartOfDay();
            case DAY -> t.toLocalDate().atStartOfDay();
            case HOUR -> t.truncatedTo(ChronoUnit.HOURS);
            case MINUTE -> t.truncatedTo(ChronoUnit.MINUTES);
            case SECOND -> t.truncatedTo(ChronoUnit.SECONDS);
        };
    }

    private LocalDateTime dt(Object v, boolean inclusive, long unitSeconds, boolean lower) {
        LocalDateTime t = switch (v) {
            case LocalDateTime x -> x;
            case LocalDate d -> d.atStartOfDay();
            case OffsetDateTime o -> LocalDateTime.ofInstant(o.toInstant(), clock.zone());
            case null, default -> null;
        };
        if (t == null || inclusive) return t;
        return lower ? t.plusSeconds(unitSeconds) : t.minusSeconds(unitSeconds);
    }

    private Object text(ColumnDef col, TypeKind kind, int typmod, ColumnRules rules, long n, Uniqueness u) {
        int maxLen = Typmod.isSet(typmod) ? Typmod.length(typmod) : Integer.MAX_VALUE;
        if (rules.maxLen != null) maxLen = Math.min(maxLen, rules.maxLen);
        int minLen = rules.minLen == null ? 1 : Math.max(1, rules.minLen);
        if (minLen > maxLen) {
            throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "no text of length " + minLen + " or more fits column " + col.qualifiedName()
                    + " (at most " + maxLen + " characters)" + HINT);
        }
        String s;
        if (!rules.likes.isEmpty()) {
            String pattern = rules.likes.size() > 1 ? mergeLikes(rules.likes) : rules.likes.get(0);
            if (pattern == null) {
                throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "column " + col.qualifiedName() + " must match LIKE patterns "
                        + rules.likes + ", which Rowsmith cannot combine" + HINT);
            }
            s = fromLike(pattern, n, minLen, maxLen);
            if (s == null) {
                throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "no value of column " + col.qualifiedName() + " matches LIKE '" + pattern
                        + "' within " + minLen + " to " + maxLen + " characters" + HINT);
            }
        } else if (!rules.regexes.isEmpty()) {
            s = fromRegex(col, rules.regexes.get(0), n);
            if (s.codePointCount(0, s.length()) > maxLen) {
                throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "the match \"" + s + "\" of the regex '" + rules.regexes.get(0)
                        + "' is longer than column " + col.qualifiedName() + " allows (" + maxLen + " characters)" + HINT);
            }
        } else {
            boolean fixedWidth = kind == TypeKind.CHAR && maxLen < Integer.MAX_VALUE;
            int width = fixedWidth ? maxLen : minLen;
            s = numbered(col.name + "-", n, width, maxLen);
            if (s == null) s = numbered("", n, width, maxLen);
            if (s == null && u != Uniqueness.ALONE) s = numbered("", lastDigits(n, maxLen), width, maxLen);
            if (s == null) {
                throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "row number " + n + " does not fit column " + col.qualifiedName()
                        + " (at most " + maxLen + " characters)" + HINT);
            }
        }
        if (rules.letterCase == ColumnRules.LetterCase.LOWER) s = s.toLowerCase(Locale.ROOT);
        else if (rules.letterCase == ColumnRules.LetterCase.UPPER) s = s.toUpperCase(Locale.ROOT);
        return s;
    }

    private static long lastDigits(long n, int count) {
        return BigInteger.valueOf(n).mod(BigInteger.TEN.pow(count)).longValue();
    }

    private static String numbered(String prefix, long n, int minLen, int maxLen) {
        String digits = Long.toString(n);
        int pad = minLen - prefix.length() - digits.length();
        String s = prefix + "0".repeat(Math.max(0, pad)) + digits;
        return s.length() <= maxLen ? s : null;
    }

    private static String fromLike(String pattern, long n, int minLen, int maxLen) {
        boolean hasPercent = false;
        int fixed = 0;
        int underscores = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '%') {
                hasPercent = true;
                continue;
            }
            fixed++;
            if (c == '\\') i++;
            else if (c == '_') underscores++;
        }
        String number = "";
        if (hasPercent) {
            number = String.format("%0" + Math.max(1, minLen - fixed) + "d", n);
            if (fixed + number.length() > maxLen) number = "";
        }
        String digits = hasPercent ? "" : String.format("%0" + Math.max(1, underscores) + "d", n);
        int nextDigit = Math.max(0, digits.length() - underscores);
        StringBuilder sb = new StringBuilder();
        boolean numberWritten = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\' && i + 1 < pattern.length()) {
                sb.append(pattern.charAt(++i));
            } else if (c == '%') {
                if (!numberWritten) sb.append(number);
                numberWritten = true;
            } else if (c == '_') {
                sb.append(hasPercent ? 'x' : digits.charAt(nextDigit++));
            } else {
                sb.append(c);
            }
        }
        return sb.length() >= minLen && sb.length() <= maxLen ? sb.toString() : null;
    }

    private static String mergeLikes(List<String> likes) {
        List<Character> merged = new ArrayList<>();
        boolean open = true;
        Integer exact = null;
        for (String p : likes) {
            List<Character> chars = new ArrayList<>();
            boolean trailing = false;
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (c == '%') {
                    if (i != p.length() - 1) return null;
                    trailing = true;
                } else if (c == '\\' && i + 1 < p.length()) {
                    chars.add(p.charAt(++i));
                } else if (c == '_') {
                    chars.add(ANY_CHAR);
                } else {
                    chars.add(c);
                }
            }
            if (!trailing) {
                if (exact != null && exact != chars.size()) return null;
                exact = chars.size();
                open = false;
            }
            for (int i = 0; i < chars.size(); i++) {
                if (i == merged.size()) merged.add(chars.get(i));
                else if (isAnyChar(merged.get(i))) merged.set(i, chars.get(i));
                else if (!isAnyChar(chars.get(i)) && !merged.get(i).equals(chars.get(i))) return null;
            }
        }
        if (exact != null && merged.size() > exact) return null;
        StringBuilder sb = new StringBuilder();
        for (Character c : merged) {
            if (isAnyChar(c)) sb.append('_');
            else if (c == '%' || c == '_' || c == '\\') sb.append('\\').append(c);
            else sb.append(c);
        }
        return open ? sb.append('%').toString() : sb.toString();
    }

    private static boolean isAnyChar(Character c) {
        return c == null;
    }

    private String fromRegex(ColumnDef col, String regex, long n) {
        String re = regex;
        if (re.startsWith("^")) re = re.substring(1);
        if (re.endsWith("$") && !re.endsWith("\\$")) re = re.substring(0, re.length() - 1);
        re = withoutPosixClasses(col, regex, re);
        try {
            Random random = Random.from(new SplittableRandom(Objects.hash(col.table().qualifiedName(), col.name, n)));
            return RgxGen.parse(regexProperties, re).generate(random);
        } catch (RuntimeException e) {
            throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "cannot generate a value of column " + col.qualifiedName()
                    + " matching the regex '" + regex + "' (" + e.getMessage() + ")" + HINT, e);
        }
    }

    private static String withoutPosixClasses(ColumnDef col, String regex, String re) {
        Matcher m = POSIX_CLASS.matcher(re);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String range = POSIX_CLASSES.get(m.group(1));
            if (range == null) {
                throw new RowsmithException(ErrorCode.CANNOT_GENERATE, "cannot generate a value of column " + col.qualifiedName()
                        + " matching the regex '" + regex + "': Rowsmith does not generate the class [:" + m.group(1)
                        + ":] (it generates " + String.join(", ", new TreeSet<>(POSIX_CLASSES.keySet())) + ")" + HINT);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(range));
        }
        return m.appendTail(out).toString();
    }

    private static String json(JsonKind kind, long n) {
        return switch (kind == null ? JsonKind.OBJECT : kind) {
            case OBJECT -> "{\"n\": " + n + "}";
            case ARRAY -> "[" + n + "]";
            case STRING -> "\"" + n + "\"";
            case NUMBER -> String.valueOf(n);
            case BOOLEAN -> "true";
            case NULL -> "null";
        };
    }

    private static byte[] bytes(long n, int length) {
        byte[] full = ByteBuffer.allocate(Long.BYTES).putLong(n).array();
        byte[] out = new byte[length];
        int copy = Math.min(length, Long.BYTES);
        System.arraycopy(full, Long.BYTES - copy, out, length - copy, copy);
        return out;
    }

    private static int length(ColumnRules rules, int preferred) {
        int lo = rules.minLen == null ? 0 : Math.max(0, rules.minLen);
        int hi = rules.maxLen == null ? Math.max(lo, preferred) : rules.maxLen;
        return Math.max(lo, Math.min(hi, preferred));
    }

    private static String vector(int dims, long n) {
        StringBuilder sb = new StringBuilder("[").append(n);
        for (int i = 1; i < Math.max(1, dims); i++) sb.append(",0");
        return sb.append(']').toString();
    }

    private static String bits(PgType base, int typmod, long n) {
        String b = Long.toBinaryString(n);
        if (!base.name.equals("bit")) return b;
        int width = Math.max(1, typmod);
        return b.length() >= width ? b.substring(b.length() - width) : "0".repeat(width - b.length()) + b;
    }

    private List<Object> array(ColumnDef col, PgType base, ColumnRules rules, long n, Request req, boolean top) {
        int size = top ? length(rules, 1) : 1;
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            out.add(value(col, base.element(), -1, new ColumnRules(), (n - 1) * size + 1 + i, req, false));
        }
        return out;
    }
}
