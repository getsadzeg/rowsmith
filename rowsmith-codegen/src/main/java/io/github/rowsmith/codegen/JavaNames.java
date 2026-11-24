package io.github.rowsmith.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class JavaNames {

    private JavaNames() {}

    static String type(String dbName) {
        StringBuilder sb = new StringBuilder();
        for (String w : words(dbName)) {
            int first = w.offsetByCodePoints(0, 1);
            sb.append(w.substring(0, first).toUpperCase(Locale.ROOT)).append(w.substring(first).toLowerCase(Locale.ROOT));
        }
        return identifier(sb.toString());
    }

    static String constant(String dbName) {
        return identifier(String.join("_", words(dbName)).toUpperCase(Locale.ROOT));
    }

    private static List<String> words(String s) {
        List<String> words = new ArrayList<>();
        StringBuilder w = new StringBuilder();
        int[] cps = s.codePoints().toArray();
        for (int i = 0; i < cps.length; i++) {
            int c = cps[i];
            if (!Character.isLetterOrDigit(c)) {
                flush(w, words);
                continue;
            }
            boolean caseChange = i > 0 && Character.isUpperCase(c) && Character.isLowerCase(cps[i - 1]);
            if (caseChange) flush(w, words);
            w.appendCodePoint(c);
        }
        flush(w, words);
        return words;
    }

    private static void flush(StringBuilder w, List<String> words) {
        if (!w.isEmpty()) words.add(w.toString());
        w.setLength(0);
    }

    private static String identifier(String s) {
        return s.isEmpty() ? "EMPTY" : s;
    }
}
