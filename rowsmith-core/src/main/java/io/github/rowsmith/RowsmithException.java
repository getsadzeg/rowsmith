package io.github.rowsmith;

/**
 * The only exception type Rowsmith throws on purpose. Messages start with {@code "Rowsmith: "},
 * name the table/column/constraint involved, say what is allowed, and end with the call site in
 * your code, e.g. {@code (LateFeeJobTest:14)}.
 */
public final class RowsmithException extends RuntimeException {

    private final ErrorCode code;
    private final String detail;

    public RowsmithException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public RowsmithException(ErrorCode code, String message, Throwable cause) {
        super("Rowsmith: " + message, cause);
        this.code = code;
        this.detail = message;
    }

    /** The message without its {@code "Rowsmith: "} prefix. */
    public String detail() {
        return detail;
    }

    /** Stable machine-readable code, see {@link ErrorCode}. */
    public ErrorCode code() {
        return code;
    }
}
