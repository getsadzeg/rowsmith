package io.github.rowsmith.internal.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SqlLexer {

    enum Kind { IDENT, QIDENT, NUMBER, STRING, OP, EOF }

    record Token(Kind kind, String text) {
        boolean is(String s) {
            return (kind == Kind.IDENT && text.equalsIgnoreCase(s)) || (kind == Kind.OP && text.equals(s));
        }
    }

    private static final String[] OPS = {
        "!~~*", "~~*", "!~~", "!~*", "::", "<>", "!=", "<=", ">=", "~~", "~*", "!~", "||",
        "=", "<", ">", "~", "+", "-", "*", "/", "%", "(", ")", "[", "]", ",", ".", ":"
    };

    static List<Token> lex(String s) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '\'' || ((c == 'E' || c == 'e') && i + 1 < n && s.charAt(i + 1) == '\'')) {
                boolean escape = c != '\'';
                if (escape) i++;
                StringBuilder sb = new StringBuilder();
                i++;
                while (true) {
                    if (i >= n) throw new IllegalArgumentException("unterminated string");
                    char d = s.charAt(i);
                    if (d == '\'' && i + 1 < n && s.charAt(i + 1) == '\'') { sb.append('\''); i += 2; continue; }
                    if (d == '\'') { i++; break; }
                    if (escape && d == '\\' && i + 1 < n) { sb.append(s.charAt(i + 1)); i += 2; continue; }
                    sb.append(d);
                    i++;
                }
                out.add(new Token(Kind.STRING, sb.toString()));
                continue;
            }
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (true) {
                    if (i >= n) throw new IllegalArgumentException("unterminated identifier");
                    char d = s.charAt(i);
                    if (d == '"' && i + 1 < n && s.charAt(i + 1) == '"') { sb.append('"'); i += 2; continue; }
                    if (d == '"') { i++; break; }
                    sb.append(d);
                    i++;
                }
                out.add(new Token(Kind.QIDENT, sb.toString()));
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int st = i;
                while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                    i++;
                    if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                    while (i < n && Character.isDigit(s.charAt(i))) i++;
                }
                out.add(new Token(Kind.NUMBER, s.substring(st, i)));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int st = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) == '$')) i++;
                out.add(new Token(Kind.IDENT, s.substring(st, i).toLowerCase(Locale.ROOT)));
                continue;
            }
            boolean matched = false;
            for (String op : OPS) {
                if (s.startsWith(op, i)) {
                    out.add(new Token(Kind.OP, op));
                    i += op.length();
                    matched = true;
                    break;
                }
            }
            if (!matched && "?@&#|^`".indexOf(c) >= 0) {
                // an operator the parser does not know (jsonb ?, array @>, &&): one token, so the
                // expression fails to parse but its columns can still be found
                int st = i;
                while (i < n && "+-*/<>=~!@#%^&|`?".indexOf(s.charAt(i)) >= 0) i++;
                out.add(new Token(Kind.OP, s.substring(st, i)));
                matched = true;
            }
            if (!matched) throw new IllegalArgumentException("unexpected character '" + c + "'");
        }
        out.add(new Token(Kind.EOF, ""));
        return out;
    }
}
