package rowsmith.internal.schema;

/**
 * pg_attribute.atttypmod as Postgres encodes it: negative when the type has no modifier, else the value
 * plus a 4-byte header; numeric packs the precision above 16 bits whose low 11 hold the signed scale
 * (negative since PostgreSQL 15).
 */
public final class Typmod {

    private static final int HEADER = 4;
    private static final int PRECISION_SHIFT = 16;
    private static final int PRECISION_MASK = (1 << PRECISION_SHIFT) - 1;
    private static final int SCALE_BITS = 11;
    private static final int SCALE_UNUSED_BITS = Integer.SIZE - SCALE_BITS;

    private Typmod() {}

    public static boolean isSet(int typmod) {
        return typmod >= HEADER;
    }

    public static int length(int typmod) {
        return typmod - HEADER;
    }

    public static int precision(int typmod) {
        return ((typmod - HEADER) >> PRECISION_SHIFT) & PRECISION_MASK;
    }

    public static int scale(int typmod) {
        return ((typmod - HEADER) << SCALE_UNUSED_BITS) >> SCALE_UNUSED_BITS;
    }
}
