package io.github.rowsmith;

/**
 * A typed column of a {@link Table}, usually a constant of generated code ({@code TASK.STATUS}).
 * Used with {@link RowSpec#set(Column, Object)}, {@link RowSpec#use(Column, Row)} and {@link Row#get(Column)}.
 *
 * @param <T> the Java type of the column's values
 */
public final class Column<T> {

    private final Table table;
    private final String name;
    private final Codec<T> codec;

    Column(Table table, String name, Codec<T> codec) {
        this.table = table;
        this.name = name;
        this.codec = codec;
    }

    public Table table() { return table; }

    /** The column name as in the database. */
    public String name() { return name; }

    Object toDb(T value) {
        return codec.toDb(value);
    }

    T fromDb(Object raw) {
        try {
            return codec.fromDb(raw);
        } catch (ClassCastException e) {
            throw new RowsmithException(ErrorCode.INVALID_VALUE, this + " holds a " + raw.getClass().getName()
                    + ", not the generated Java type; regenerate the code after a schema change", e);
        }
    }

    @Override
    public String toString() {
        return table.qualifiedName() + "." + name;
    }
}
