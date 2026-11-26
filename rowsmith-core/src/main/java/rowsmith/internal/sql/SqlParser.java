package rowsmith.internal.sql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import rowsmith.internal.sql.SqlExpr.*;
import rowsmith.internal.sql.SqlLexer.Kind;
import rowsmith.internal.sql.SqlLexer.Token;
import rowsmith.internal.util.Strings;

public final class SqlParser {

    private final List<Token> tokens;
    private int pos;

    private SqlParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static SqlExpr parse(String text) {
        SqlParser p = new SqlParser(SqlLexer.lex(text));
        SqlExpr e = p.or();
        if (p.peek().kind() != Kind.EOF) throw new IllegalArgumentException("unexpected '" + p.peek().text() + "'");
        return e;
    }

    public static SqlExpr tryParse(String text) {
        try {
            return parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Token peek() { return tokens.get(pos); }
    private Token peek(int k) { return tokens.get(Math.min(pos + k, tokens.size() - 1)); }
    private Token next() { return tokens.get(pos++); }

    private boolean accept(String s) {
        if (peek().is(s)) { pos++; return true; }
        return false;
    }

    private void expect(String s) {
        if (!accept(s)) throw new IllegalArgumentException("expected '" + s + "' but got '" + peek().text() + "'");
    }

    private SqlExpr or() {
        SqlExpr e = and();
        while (accept("or")) e = new Bin(Op.OR, e, and());
        return e;
    }

    private SqlExpr and() {
        SqlExpr e = not();
        while (accept("and")) e = new Bin(Op.AND, e, not());
        return e;
    }

    private SqlExpr not() {
        if (accept("not")) return new Unary(UnaryOp.NOT, not());
        return is();
    }

    private SqlExpr is() {
        SqlExpr e = cmp();
        while (true) {
            if (accept("isnull")) { e = new IsNull(e, false); continue; }
            if (accept("notnull")) { e = new IsNull(e, true); continue; }
            if (!peek().is("is")) return e;
            next();
            boolean neg = accept("not");
            if (accept("null")) e = new IsNull(e, neg);
            else if (accept("true")) e = new IsBool(e, true, neg);
            else if (accept("false")) e = new IsBool(e, false, neg);
            else if (accept("distinct")) { expect("from"); e = new Distinct(e, cmp(), neg); }
            else throw new IllegalArgumentException("unsupported IS form");
        }
    }

    private static final Set<Op> ADDITIVE = EnumSet.of(Op.ADD, Op.SUB);
    private static final Set<Op> MULTIPLICATIVE = EnumSet.of(Op.MUL, Op.DIV, Op.MOD);

    private static boolean isOther(Op op) {
        return op.isPattern() || op == Op.CONCAT;
    }

    private Optional<Op> acceptOp(Predicate<Op> which) {
        Optional<Op> op = peek().kind() == Kind.OP ? Op.of(peek().text()).filter(which) : Optional.empty();
        if (op.isPresent()) pos++;
        return op;
    }

    private SqlExpr cmp() {
        SqlExpr left = like();
        Optional<Op> cmp = acceptOp(Op::isComparison);
        if (cmp.isPresent()) {
            Op op = cmp.get();
            if ((peek().is("any") || peek().is("some") || peek().is("all")) && peek(1).is("(")) {
                boolean all = next().is("all");
                expect("(");
                SqlExpr arr = or();
                expect(")");
                return new AnyAll(op, left, arr, all);
            }
            return new Bin(op, left, like());
        }
        return left;
    }

    private SqlExpr like() {
        SqlExpr left = other();
        boolean neg = false;
        if (peek().is("not") && (peek(1).is("like") || peek(1).is("ilike") || peek(1).is("between") || peek(1).is("in"))) {
            next();
            neg = true;
        }
        if (accept("like")) return new Bin(neg ? Op.NOT_LIKE : Op.LIKE, left, other());
        if (accept("ilike")) return new Bin(neg ? Op.NOT_ILIKE : Op.ILIKE, left, other());
        if (accept("between")) {
            if (peek().is("symmetric")) throw new IllegalArgumentException("BETWEEN SYMMETRIC");
            SqlExpr lo = other();
            expect("and");
            return new Between(left, lo, other(), neg);
        }
        if (accept("in")) {
            expect("(");
            List<SqlExpr> vals = new ArrayList<>();
            do { vals.add(or()); } while (accept(","));
            expect(")");
            return new InList(left, vals, neg);
        }
        if (neg) throw new IllegalArgumentException("dangling NOT");
        return left;
    }

    private SqlExpr other() {
        SqlExpr e = add();
        for (Optional<Op> o = acceptOp(SqlParser::isOther); o.isPresent(); o = acceptOp(SqlParser::isOther)) {
            Op op = o.get();
            if ((peek().is("any") || peek().is("all")) && peek(1).is("(")) {
                boolean all = next().is("all");
                expect("(");
                SqlExpr arr = or();
                expect(")");
                e = new AnyAll(op, e, arr, all);
            } else {
                e = new Bin(op, e, add());
            }
        }
        return e;
    }

    private SqlExpr add() {
        SqlExpr e = mul();
        for (Optional<Op> o = acceptOp(ADDITIVE::contains); o.isPresent(); o = acceptOp(ADDITIVE::contains)) e = new Bin(o.get(), e, mul());
        return e;
    }

    private SqlExpr mul() {
        SqlExpr e = unary();
        for (Optional<Op> o = acceptOp(MULTIPLICATIVE::contains); o.isPresent(); o = acceptOp(MULTIPLICATIVE::contains)) e = new Bin(o.get(), e, unary());
        return e;
    }

    private SqlExpr unary() {
        if (accept("-")) {
            SqlExpr inner = unary();
            if (inner instanceof Lit l && l.value() instanceof BigDecimal b) return new Lit(b.negate());
            return new Unary(UnaryOp.NEG, inner);
        }
        if (accept("+")) return unary();
        return postfix();
    }

    private SqlExpr postfix() {
        SqlExpr e = primary();
        while (true) {
            if (accept("::")) {
                TypeName t = typeName();
                e = new Cast(e, t.name(), t.sql());
            } else if (peek().is("[")) {
                next();
                SqlExpr idx = or();
                if (peek().is(":")) throw new IllegalArgumentException("array slices unsupported");
                expect("]");
                e = new Subscript(e, idx);
            } else {
                return e;
            }
        }
    }

    record TypeName(String name, String sql) {}

    TypeName typeName() {
        StringBuilder name = new StringBuilder();
        StringBuilder sql = new StringBuilder();
        Token t = next();
        if (t.kind() != Kind.IDENT && t.kind() != Kind.QIDENT) throw new IllegalArgumentException("type expected");
        name.append(t.text());
        sql.append(identSql(t));
        while (peek().is(".")) {
            next();
            Token part = next();
            name.append('.').append(part.text());
            sql.append('.').append(identSql(part));
        }
        String first = t.text();
        String words = "";
        if (first.equals("character") && accept("varying")) words = " varying";
        else if (first.equals("double") && accept("precision")) words = " precision";
        else if (first.equals("bit") && accept("varying")) words = " varying";
        name.append(words);
        sql.append(words);
        if (first.equals("timestamp") || first.equals("time")) {
            if (peek().is("(")) sql.append(parens());
            String zone = "";
            if (accept("with")) { expect("time"); expect("zone"); zone = " with time zone"; }
            else if (accept("without")) { expect("time"); expect("zone"); zone = " without time zone"; }
            name.append(zone);
            sql.append(zone);
        }
        if (peek().is("(")) sql.append(parens());
        while (peek().is("[")) {
            next();
            if (peek().kind() == Kind.NUMBER) next();
            expect("]");
            name.append("[]");
            sql.append("[]");
        }
        return new TypeName(name.toString(), sql.toString());
    }

    private static String identSql(Token t) {
        return t.kind() == Kind.QIDENT ? Strings.quoteIdent(t.text()) : t.text();
    }

    private String parens() {
        expect("(");
        StringBuilder sb = new StringBuilder("(");
        int depth = 1;
        while (depth > 0) {
            Token t = next();
            if (t.kind() == Kind.EOF) throw new IllegalArgumentException("unbalanced");
            if (t.is("(")) depth++;
            if (t.is(")")) depth--;
            sb.append(t.kind() == Kind.STRING ? Strings.quoteLiteral(t.text()) : t.text());
        }
        return sb.toString();
    }

    private static final Set<String> TYPED_LITERALS = Set.of("date", "timestamp", "timestamptz", "time", "interval",
            "numeric", "text", "uuid", "jsonb", "json", "boolean", "int", "integer", "bigint");

    private SqlExpr primary() {
        Token t = next();
        switch (t.kind()) {
            case NUMBER -> { return new Lit(new BigDecimal(t.text())); }
            case STRING -> { return new Lit(t.text()); }
            case QIDENT -> {
                // Postgres quotes some function names it deparses, e.g. "substring"(VALUE, 1, 1)
                if (peek().is("(")) {
                    next();
                    return function(null, t.text());
                }
                return qualified(t.text());
            }
            case OP -> {
                if (t.is("(")) {
                    SqlExpr e = or();
                    if (peek().is(",")) throw new IllegalArgumentException("row constructors unsupported");
                    expect(")");
                    return e;
                }
                throw new IllegalArgumentException("unexpected '" + t.text() + "'");
            }
            case IDENT -> { return identStart(t.text()); }
            default -> throw new IllegalArgumentException("unexpected end");
        }
    }

    private SqlExpr identStart(String word) {
        return switch (word) {
            case "null" -> new Lit(null);
            case "true" -> new Lit(Boolean.TRUE);
            case "false" -> new Lit(Boolean.FALSE);
            case "array" -> {
                expect("[");
                List<SqlExpr> els = new ArrayList<>();
                if (!peek().is("]")) {
                    do { els.add(or()); } while (accept(","));
                }
                expect("]");
                yield new ArrayLit(els);
            }
            case "current_date", "current_timestamp", "localtimestamp", "current_time",
                    "localtime", "current_user", "session_user", "current_schema" -> new Keyword(peek().is("(") ? word + parens() : word);
            case "case" -> caseExpr();
            case "cast" -> {
                expect("(");
                SqlExpr e = or();
                expect("as");
                TypeName type = typeName();
                expect(")");
                yield new Cast(e, type.name(), type.sql());
            }
            default -> identOrCall(word);
        };
    }

    private SqlExpr identOrCall(String word) {
        if (peek().kind() == Kind.STRING && TYPED_LITERALS.contains(word)) {
            return new Cast(new Lit(next().text()), word, word);
        }
        if (peek().is("(")) {
            next();
            return function(null, word);
        }
        return qualified(word);
    }

    private SqlExpr caseExpr() {
        SqlExpr operand = peek().is("when") ? null : or();
        List<SqlExpr> whens = new ArrayList<>();
        List<SqlExpr> thens = new ArrayList<>();
        while (accept("when")) {
            whens.add(or());
            expect("then");
            thens.add(or());
        }
        if (whens.isEmpty()) throw new IllegalArgumentException("CASE without WHEN");
        SqlExpr otherwise = accept("else") ? or() : null;
        expect("end");
        return new Case(operand, whens, thens, otherwise);
    }

    private SqlExpr function(String schema, String name) {
        List<SqlExpr> args = new ArrayList<>();
        if (name.equals("extract") && (peek().kind() == Kind.IDENT || peek().kind() == Kind.STRING) && peek(1).is("from")) {
            String field = next().text();
            next();
            SqlExpr source = or();
            expect(")");
            return new Func("date_part", List.of(new Lit(field.toLowerCase(Locale.ROOT)), source));
        }
        if (name.equals("substring")) {
            SqlExpr target = or();
            if (accept("from")) {
                args.add(target);
                args.add(or());
                if (accept("for")) args.add(or());
                expect(")");
                return new Func("substring", args);
            }
            args.add(target);
            while (accept(",")) args.add(or());
            expect(")");
            return new Func("substring", args);
        }
        if (name.equals("trim") || name.equals("btrim")) {
            String mode = "both";
            if (peek().is("both") || peek().is("leading") || peek().is("trailing")) mode = next().text();
            SqlExpr chars = null;
            if (!peek().is("from")) {
                SqlExpr first = or();
                if (!accept("from")) {
                    args.add(first);
                    while (accept(",")) args.add(or());
                    expect(")");
                    return new Func("btrim", args);
                }
                chars = first;
            } else {
                next();
            }
            SqlExpr target = or();
            expect(")");
            String fn = switch (mode) { case "leading" -> "ltrim"; case "trailing" -> "rtrim"; default -> "btrim"; };
            return chars == null ? new Func(fn, List.of(target)) : new Func(fn, List.of(target, chars));
        }
        if (accept(")")) return new Func(schema, name, args);
        if (accept("distinct")) throw new IllegalArgumentException("aggregate");
        do {
            args.add(or());
        } while (accept(","));
        if (peek().is("from") || peek().is("in") || peek().is("order")) throw new IllegalArgumentException("special function syntax");
        expect(")");
        return new Func(schema, name, args);
    }

    private SqlExpr qualified(String first) {
        String name = first;
        while (peek().is(".")) {
            next();
            Token t = next();
            if (t.kind() != Kind.IDENT && t.kind() != Kind.QIDENT) throw new IllegalArgumentException("identifier expected");
            String qualifier = name;
            name = t.text();
            if (peek().is("(")) {
                next();
                return function(qualifier, name);
            }
        }
        return new Col(name);
    }
}
