package io.github.rowsmith;

/**
 * Stable error codes carried by every {@link RowsmithException}, each documented below; messages may change
 * between versions, codes will not.
 */
public enum ErrorCode {
    /** A table name does not exist in the database; the message lists the tables. */
    UNKNOWN_TABLE,
    /** A column name does not exist on the table; the message lists its columns. */
    UNKNOWN_COLUMN,
    /** A value cannot be converted to the column type (e.g. "abc" for a numeric column). */
    INVALID_VALUE,
    /** A value is too long for a varchar(n)/char(n) column or overflows numeric(p,s). */
    VALUE_OUT_OF_RANGE,
    /** A value is not one of the labels of a Postgres enum. */
    NOT_IN_ENUM,
    /** A value (or combination of values) violates a CHECK constraint. */
    CHECK_VIOLATION,
    /** A NOT NULL column was explicitly set to null. */
    NOT_NULL_VIOLATION,
    /** The requested rows would violate a unique constraint or unique index (incl. partial). */
    UNIQUE_VIOLATION,
    /** A foreign key value does not reference an existing row. */
    FOREIGN_KEY_VIOLATION,
    /** Required (NOT NULL, non-deferrable) foreign keys form a cycle; rows cannot be ordered. */
    REQUIRED_FK_CYCLE,
    /** A relationship (with/use) is ambiguous or impossible, e.g. the child has no FK to the parent. */
    INVALID_RELATIONSHIP,
    /** The table has no single-column primary key, so a row cannot be addressed by one id ({@code find}, {@code id()}). */
    NO_SINGLE_PRIMARY_KEY,
    /** {@code find} found no row with the given primary key. */
    ROW_NOT_FOUND,
    /** Rowsmith could not generate a value satisfying the column's constraints; set it explicitly. */
    CANNOT_GENERATE,
    /** The column type is not supported for generation; set it explicitly or give it a default. */
    UNSUPPORTED_TYPE,
    /**
     * A setting is invalid, e.g. a {@link RowsmithSetup} value pattern for a foreign key or generated column,
     * or several Clock beans in a Spring test context with none {@code @Primary}.
     */
    INVALID_CONFIG,
    /** A database operation failed in a way Rowsmith did not predict, e.g. an insert, a catalog read or a cleanup statement. */
    DATABASE_ERROR,
    /**
     * Cleanup refused and deleted nothing: rows Rowsmith did not create still reference rows it created, a table
     * has no primary key or NOT NULL unique key to identify rows by, or a transaction is open on this thread.
     */
    CLEANUP_BLOCKED,
    /** No DataSource could be found for the test. */
    NO_DATASOURCE
}
