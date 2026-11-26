package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WitnessCheckTest {

    enum Kind { INT, NUMERIC, TEXT, VARCHAR, DATE, BOOL }

    record Col(String name, Kind kind, boolean notNull, String type, Object value) {
        String lit() { return literal(kind, value); }
    }

    record Schema(String ddl, String witnessInsert, List<String> checks) {}

    static IntStream seeds() {
        return IntStream.rangeClosed(1, 60);
    }

    @ParameterizedTest
    @MethodSource("seeds")
    void checksWithAWitnessSeedOrFailClearly(int seed) {
        Schema s = generate(new Random(seed));
        DataSource ds = Pg.withSchema(s.ddl());
        Pg.exec(ds, s.witnessInsert() + "; DELETE FROM w;");
        RealWorldCheckTest.Case c = new RealWorldCheckTest.Case("witness", "witness/" + seed, "-", "-", RealWorldCheckTest.Outcome.OK, 0, s.ddl());
        RealWorldCheckTest.Result r = RealWorldCheckTest.run(ds, c, "public", "w");
        assertThat(r.outcome()).as("seed %s%n%s%n%s", seed, s.ddl(), r.detail()).isNotEqualTo(RealWorldCheckTest.Outcome.BAD);
    }

    static Schema generate(Random r) {
        int n = 2 + r.nextInt(4);
        List<Col> cols = new ArrayList<>();
        for (int i = 0; i < n; i++) cols.add(column(r, "c" + i));
        List<String> checks = new ArrayList<>();
        int nChecks = 1 + r.nextInt(4);
        for (int i = 0; i < nChecks; i++) checks.add(check(r, cols, 0));
        StringBuilder ddl = new StringBuilder("CREATE TABLE w (\n  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY");
        for (Col c : cols) ddl.append(",\n  ").append(c.name()).append(' ').append(c.type()).append(c.notNull() ? " NOT NULL" : "");
        for (String ch : checks) ddl.append(",\n  CHECK (").append(ch).append(')');
        ddl.append("\n);\n");
        List<String> names = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Col c : cols) {
            names.add(c.name());
            values.add(c.lit());
        }
        String insert = "INSERT INTO w (" + String.join(", ", names) + ") VALUES (" + String.join(", ", values) + ")";
        return new Schema(ddl.toString(), insert, checks);
    }

    private static Col column(Random r, String name) {
        Kind k = Kind.values()[r.nextInt(Kind.values().length)];
        boolean notNull = r.nextInt(3) > 0;
        boolean isNull = !notNull && r.nextInt(4) == 0;
        Object v = value(r, k);
        String type = switch (k) {
            case INT -> "integer";
            case NUMERIC -> "numeric(10,2)";
            case TEXT -> "text";
            case VARCHAR -> "varchar(" + (((String) v).length() + r.nextInt(10)) + ")";
            case DATE -> "date";
            case BOOL -> "boolean";
        };
        return new Col(name, k, notNull, type, isNull ? null : v);
    }

    private static Object value(Random r, Kind k) {
        return switch (k) {
            case INT -> (long) (r.nextInt(2001) - 1000);
            case NUMERIC -> BigDecimal.valueOf(r.nextInt(2_000_000) - 1_000_000, 2);
            case TEXT, VARCHAR -> word(r, 1 + r.nextInt(12));
            case DATE -> LocalDate.of(2000, 1, 1).plusDays(r.nextInt(12_000));
            case BOOL -> r.nextBoolean();
        };
    }

    private static String word(Random r, int len) {
        String alphabet = r.nextBoolean() ? "abcdefghijklmnopqrstuvwxyz" : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_ ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        String s = sb.toString().strip();
        return s.isEmpty() ? "x" : s;
    }

    static String check(Random r, List<Col> cols, int depth) {
        int pick = depth >= 2 ? 0 : r.nextInt(12);
        return switch (pick) {
            case 6 -> "(" + check(r, cols, depth + 1) + " AND " + check(r, cols, depth + 1) + ")";
            case 7 -> r.nextBoolean() ? "(" + check(r, cols, depth + 1) + " OR " + anyAtom(r, cols) + ")"
                    : "(" + anyAtom(r, cols) + " OR " + check(r, cols, depth + 1) + ")";
            case 8 -> "NOT (" + falseAtom(r, cols) + ")";
            case 9 -> crossColumn(r, cols);
            case 10 -> r.nextBoolean()
                    ? "CASE WHEN " + atom(r, cols.get(r.nextInt(cols.size()))) + " THEN " + check(r, cols, depth + 1) + " ELSE " + anyAtom(r, cols) + " END"
                    : "CASE WHEN " + falseAtom(r, cols) + " THEN " + anyAtom(r, cols) + " ELSE " + check(r, cols, depth + 1) + " END";
            case 11 -> sum(r, cols);
            default -> atom(r, cols.get(r.nextInt(cols.size())));
        };
    }

    private static String anyAtom(Random r, List<Col> cols) {
        Col c = cols.get(r.nextInt(cols.size()));
        if (r.nextInt(5) == 0) return c.name() + " IS NULL";
        return atom(r, new Col(c.name(), c.kind(), c.notNull(), c.type(), value(r, c.kind())));
    }

    private static String falseAtom(Random r, List<Col> cols) {
        Col c = cols.get(r.nextInt(cols.size()));
        if (c.value() == null) return c.name() + " IS NOT NULL";
        return switch (c.kind()) {
            case INT, NUMERIC -> c.name() + " = " + literal(c.kind(), plus(c, 1 + r.nextInt(50)));
            case TEXT, VARCHAR -> c.name() + " = " + literal(c.kind(), c.value() + "z");
            case DATE -> c.name() + " < " + literal(c.kind(), ((LocalDate) c.value()).minusDays(1 + r.nextInt(100)));
            case BOOL -> c.name() + " = " + !((Boolean) c.value());
        };
    }

    private static String atom(Random r, Col c) {
        String n = c.name();
        if (c.value() == null) {
            return switch (r.nextInt(3)) {
                case 0 -> n + " IS NULL";
                case 1 -> "(" + n + " IS NULL OR " + n + " IS NOT NULL)";
                default -> "(" + n + " IS NULL OR " + someValueAtom(r, c) + ")";
            };
        }
        String nullable = !c.notNull() && r.nextInt(4) == 0 ? n + " IS NULL OR " : "";
        String a = switch (c.kind()) {
            case INT, NUMERIC -> switch (r.nextInt(6)) {
                case 0 -> n + " >= " + literal(c.kind(), plus(c, -r.nextInt(100)));
                case 1 -> n + " < " + literal(c.kind(), plus(c, 1 + r.nextInt(100)));
                case 2 -> n + " BETWEEN " + literal(c.kind(), plus(c, -r.nextInt(30))) + " AND " + literal(c.kind(), plus(c, r.nextInt(30)));
                case 3 -> n + " IN (" + literal(c.kind(), plus(c, 7)) + ", " + c.lit() + ", " + literal(c.kind(), plus(c, -3)) + ")";
                case 4 -> n + " <> " + literal(c.kind(), plus(c, 1 + r.nextInt(5)));
                default -> "(" + n + " > " + literal(c.kind(), plus(c, -1 - r.nextInt(10))) + " AND " + n + " <= " + literal(c.kind(), plus(c, r.nextInt(10))) + ")";
            };
            case TEXT, VARCHAR -> {
                String v = (String) c.value();
                yield switch (r.nextInt(8)) {
                    case 0 -> "length(" + n + ") <= " + (v.length() + r.nextInt(5));
                    case 1 -> "char_length(" + n + ") BETWEEN " + Math.max(1, v.length() - r.nextInt(3)) + " AND " + (v.length() + r.nextInt(3));
                    case 2 -> n + " IN ('" + v + "', 'other', 'third')";
                    case 3 -> n + " LIKE '" + v.substring(0, 1 + r.nextInt(v.length())).replace("_", "\\_").replace("%", "\\%") + "%'";
                    case 4 -> v.matches("[a-z]+") ? n + " ~ '^[a-z]+$'" : n + " ~ '^[A-Za-z0-9_ -]+$'";
                    case 5 -> v.equals(v.toLowerCase(Locale.ROOT)) ? n + " = lower(" + n + ")" : n + " <> ''";
                    case 6 -> "btrim(" + n + ") <> ''";
                    default -> n + " <> '" + v + "x'";
                };
            }
            case DATE -> switch (r.nextInt(3)) {
                case 0 -> n + " >= DATE '" + ((LocalDate) c.value()).minusDays(r.nextInt(400)) + "'";
                case 1 -> n + " < '" + ((LocalDate) c.value()).plusDays(1 + r.nextInt(400)) + "'::date";
                default -> n + " BETWEEN '" + ((LocalDate) c.value()).minusDays(r.nextInt(40)) + "' AND '" + ((LocalDate) c.value()).plusDays(r.nextInt(40)) + "'";
            };
            case BOOL -> r.nextBoolean() ? n + " = " + c.value() : n + " IS " + (((Boolean) c.value()) ? "TRUE" : "FALSE");
        };
        return nullable.isEmpty() ? a : "(" + nullable + a + ")";
    }

    private static String someValueAtom(Random r, Col c) {
        return atom(r, new Col(c.name(), c.kind(), c.notNull(), c.type(), value(r, c.kind())));
    }

    private static String crossColumn(Random r, List<Col> cols) {
        Col a = cols.get(r.nextInt(cols.size()));
        Col b = cols.get(r.nextInt(cols.size()));
        if (a == b) return atom(r, a);
        if (a.value() == null || b.value() == null) {
            String ia = a.name() + (a.value() == null ? " IS NULL" : " IS NOT NULL");
            String ib = b.name() + (b.value() == null ? " IS NULL" : " IS NOT NULL");
            return r.nextBoolean() ? "((" + ia + ") = (" + ib + "))" : "num_nulls(" + a.name() + ", " + b.name() + ") = "
                    + ((a.value() == null ? 1 : 0) + (b.value() == null ? 1 : 0));
        }
        boolean comparable = a.kind() == b.kind() || (isNum(a) && isNum(b)) || (isText(a) && isText(b));
        if (!comparable) {
            return "(" + a.name() + " IS NOT NULL) = (" + b.name() + " IS NOT NULL)";
        }
        int cmp = compare(a, b);
        if (isText(a)) return collationIndependentComparison(a, b, cmp);
        String op = cmp < 0 ? (r.nextBoolean() ? "<" : "<=") : cmp > 0 ? (r.nextBoolean() ? ">" : ">=") : (r.nextBoolean() ? "=" : "<=");
        if (cmp != 0 && r.nextInt(3) == 0) op = "<>";
        return a.name() + " " + op + " " + b.name();
    }

    private static String collationIndependentComparison(Col a, Col b, int cmp) {
        return a.name() + (cmp == 0 ? " = " : " <> ") + b.name();
    }

    private static String sum(Random r, List<Col> cols) {
        Col a = cols.get(r.nextInt(cols.size()));
        Col b = cols.get(r.nextInt(cols.size()));
        if (a == b || !isNum(a) || !isNum(b) || a.value() == null || b.value() == null) {
            int nonNull = (a.value() == null ? 0 : 1) + (a == b ? 0 : b.value() == null ? 0 : 1);
            return a == b ? atom(r, a) : "num_nonnulls(" + a.name() + ", " + b.name() + ") >= " + nonNull;
        }
        BigDecimal s = new BigDecimal(a.value().toString()).add(new BigDecimal(b.value().toString()));
        return r.nextBoolean() ? a.name() + " + " + b.name() + " <= " + s.add(BigDecimal.valueOf(r.nextInt(50))).toPlainString()
                : a.name() + " + " + b.name() + " > " + s.subtract(BigDecimal.valueOf(1 + r.nextInt(50))).toPlainString();
    }

    private static boolean isNum(Col c) { return c.kind() == Kind.INT || c.kind() == Kind.NUMERIC; }

    private static boolean isText(Col c) { return c.kind() == Kind.TEXT || c.kind() == Kind.VARCHAR; }

    @SuppressWarnings("unchecked")
    private static int compare(Col a, Col b) {
        if (isNum(a)) return new BigDecimal(a.value().toString()).compareTo(new BigDecimal(b.value().toString()));
        if (isText(a)) return a.value().equals(b.value()) ? 0 : 1;
        return ((Comparable<Object>) a.value()).compareTo(b.value());
    }

    private static Object plus(Col c, int delta) {
        if (c.kind() == Kind.INT) return (Long) c.value() + delta;
        return ((BigDecimal) c.value()).add(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.UNNECESSARY);
    }

    static String literal(Kind k, Object v) {
        if (v == null) return "NULL";
        return switch (k) {
            case INT, NUMERIC, BOOL -> v.toString();
            case TEXT, VARCHAR -> "'" + v.toString().replace("'", "''") + "'";
            case DATE -> "DATE '" + v + "'";
        };
    }
}
