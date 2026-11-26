package rowsmith.internal.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

public final class Savepoints {

    @FunctionalInterface
    public interface SqlWork<T> {
        T run() throws SQLException;
    }

    private enum OnSuccess { KEEP, UNDO }

    private Savepoints() {}

    public static <T> T isolated(Connection conn, SqlWork<T> work) throws SQLException {
        return conn.getAutoCommit() ? work.run() : inSavepoint(conn, work, OnSuccess.KEEP);
    }

    public static <T> T undone(Connection conn, SqlWork<T> work) throws SQLException {
        return inSavepoint(conn, work, OnSuccess.UNDO);
    }

    private static <T> T inSavepoint(Connection conn, SqlWork<T> work, OnSuccess onSuccess) throws SQLException {
        Savepoint savepoint = conn.setSavepoint();
        boolean keep = false;
        try {
            T result = work.run();
            keep = onSuccess == OnSuccess.KEEP;
            return result;
        } finally {
            if (!keep) conn.rollback(savepoint);
            conn.releaseSavepoint(savepoint);
        }
    }
}
