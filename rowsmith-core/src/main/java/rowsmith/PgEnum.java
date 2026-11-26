package rowsmith;

/**
 * A Java enum whose constants stand for database values: the labels of a Postgres enum type, or the
 * values a CHECK constraint allows for a text column. Generated code implements it; Rowsmith writes
 * {@link #label()} to the database.
 */
public interface PgEnum {

    /** The value as stored in the database. */
    String label();
}
