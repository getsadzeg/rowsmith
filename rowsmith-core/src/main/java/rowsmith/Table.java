package rowsmith;

import java.util.List;

/**
 * A database table as a typed constant, usually generated ({@code public static final Task TASK}).
 * Subclasses declare their columns as {@code public final Column<T>} fields created with the
 * factory methods below.
 */
public abstract class Table {

    private final String schema;
    private final String name;

    protected Table(String schema, String name) {
        this.schema = schema;
        this.name = name;
    }

    public final String schema() { return schema; }

    public final String name() { return name; }

    /** {@code schema.name}, independent of the search_path. */
    public final String qualifiedName() { return schema + "." + name; }

    /** A column whose values Rowsmith reads as {@code type} (Long, String, LocalDate, UUID, byte[], ...). */
    protected final <T> Column<T> column(String column, Class<T> type) {
        return new Column<>(this, column, new Codec.Plain<>(type));
    }

    /** A Postgres enum column, or a text column whose CHECK allows only the labels of {@code type}. */
    protected final <E extends Enum<E> & PgEnum> Column<E> enumColumn(String column, Class<E> type) {
        return new Column<>(this, column, new Codec.Labels<>(type));
    }

    /** An array column. */
    protected final <E> Column<List<E>> listColumn(String column, Class<E> elementType) {
        return new Column<>(this, column, new Codec.ListOf<>(new Codec.Plain<>(elementType)));
    }

    /** An array column of a Postgres enum. */
    protected final <E extends Enum<E> & PgEnum> Column<List<E>> enumListColumn(String column, Class<E> elementType) {
        return new Column<>(this, column, new Codec.ListOf<>(new Codec.Labels<>(elementType)));
    }

    @Override
    public String toString() {
        return qualifiedName();
    }
}
