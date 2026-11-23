package io.github.rowsmith.internal.gen;

import io.github.rowsmith.internal.util.Strings;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;

public final class Literals {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
    private static final DateTimeFormatter TSTZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]xxx");

    private Literals() {}

    public static String toText(Object v) {
        if (v == null) return null;
        if (v instanceof String s) return s;
        if (v instanceof BigDecimal b) return b.toPlainString();
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof LocalDateTime t) return TS.format(t);
        if (v instanceof OffsetDateTime t) return TSTZ.format(t);
        if (v instanceof byte[] bytes) return "\\x" + HexFormat.of().formatHex(bytes);
        if (v instanceof Collection<?> c) return array(c);
        if (v instanceof Object[] arr) return array(List.of(arr));
        return v.toString();
    }

    private static String array(Collection<?> c) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            Object e = it.next();
            if (e == null) sb.append("NULL");
            else if (e instanceof Collection<?> inner) sb.append(array(inner));
            else sb.append('"').append(toText(e).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            if (it.hasNext()) sb.append(',');
        }
        return sb.append('}').toString();
    }

    public static String sql(Object v, String castType) {
        if (v == null) return "NULL";
        return Strings.quoteLiteral(toText(v)) + "::" + castType;
    }
}
