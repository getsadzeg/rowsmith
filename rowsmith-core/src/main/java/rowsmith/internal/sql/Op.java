package rowsmith.internal.sql;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Op {
    OR("OR"), AND("AND"),
    EQ("="), NE("<>"), LT("<"), LE("<="), GT(">"), GE(">="),
    LIKE("~~"), NOT_LIKE("!~~"), ILIKE("~~*"), NOT_ILIKE("!~~*"),
    REGEX("~"), NOT_REGEX("!~"), IREGEX("~*"), NOT_IREGEX("!~*"),
    CONCAT("||"), ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%");

    public final String sql;

    private static final Map<String, Op> BY_SQL = Arrays.stream(values()).collect(Collectors.toMap(op -> op.sql, Function.identity()));

    Op(String sql) {
        this.sql = sql;
    }

    public static Optional<Op> of(String text) {
        return Optional.ofNullable(text.equals("!=") ? NE : BY_SQL.get(text));
    }

    public boolean isComparison() {
        return switch (this) {
            case EQ, NE, LT, LE, GT, GE -> true;
            case OR, AND, LIKE, NOT_LIKE, ILIKE, NOT_ILIKE, REGEX, NOT_REGEX, IREGEX, NOT_IREGEX, CONCAT, ADD, SUB, MUL, DIV, MOD -> false;
        };
    }

    public boolean isPattern() {
        return switch (this) {
            case LIKE, NOT_LIKE, ILIKE, NOT_ILIKE, REGEX, NOT_REGEX, IREGEX, NOT_IREGEX -> true;
            case OR, AND, EQ, NE, LT, LE, GT, GE, CONCAT, ADD, SUB, MUL, DIV, MOD -> false;
        };
    }

    public Op flip() {
        return switch (this) {
            case EQ, NE -> this;
            case LT -> GT;
            case LE -> GE;
            case GT -> LT;
            case GE -> LE;
            case OR, AND, LIKE, NOT_LIKE, ILIKE, NOT_ILIKE, REGEX, NOT_REGEX, IREGEX, NOT_IREGEX, CONCAT, ADD, SUB, MUL, DIV, MOD ->
                    throw new IllegalStateException(sql + " is not a comparison");
        };
    }

    public Op negate() {
        return switch (this) {
            case EQ -> NE;
            case NE -> EQ;
            case LT -> GE;
            case LE -> GT;
            case GT -> LE;
            case GE -> LT;
            case LIKE -> NOT_LIKE;
            case NOT_LIKE -> LIKE;
            case ILIKE -> NOT_ILIKE;
            case NOT_ILIKE -> ILIKE;
            case REGEX -> NOT_REGEX;
            case NOT_REGEX -> REGEX;
            case IREGEX -> NOT_IREGEX;
            case NOT_IREGEX -> IREGEX;
            case OR, AND, CONCAT, ADD, SUB, MUL, DIV, MOD -> throw new IllegalStateException(sql + " has no negated operator");
        };
    }
}
