package rowsmith.internal.util;

import java.util.List;

public final class Strings {

    private Strings() {}

    public static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    public static String quoteLiteral(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    public static String joinLimited(List<String> values, int max) {
        if (values.size() <= max) return String.join(", ", values);
        return String.join(", ", values.subList(0, max)) + ", ... (" + (values.size() - max) + " more)";
    }
}
