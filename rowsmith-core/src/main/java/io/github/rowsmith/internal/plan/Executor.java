package io.github.rowsmith.internal.plan;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.internal.gen.TypeKind;
import io.github.rowsmith.internal.schema.ColumnDef;
import io.github.rowsmith.internal.schema.PgType;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.postgresql.util.PSQLState;

final class Executor {

    static String castFor(PgType t, ColumnDef col) {
        if (t.isArray()) return castFor(t.element(), null) + "[]";
        if (!t.schema.equals("pg_catalog")) return t.castName;
        return switch (t.name) {
            case "bpchar" -> "bpchar";
            case "varchar" -> "varchar";
            case "bit" -> col != null ? col.formattedType : "varbit";
            default -> t.castName;
        };
    }

    static Object readValue(ResultSet rs, int i, ColumnDef c) throws SQLException {
        return readValue(rs, i, c.type);
    }

    private static Object readValue(ResultSet rs, int i, PgType type) throws SQLException {
        TypeKind kind = TypeKind.of(type);
        return kind == TypeKind.ARRAY ? readArray(rs.getArray(i), type) : kind.read(rs, i);
    }

    private static List<Object> readArray(Array a, PgType type) throws SQLException {
        if (a == null) return null;
        try (ResultSet elements = a.getResultSet()) {
            PgType elementType = elements.getMetaData().getColumnType(2) == Types.ARRAY ? type : TypeKind.strip(type).element();
            List<Object> list = new ArrayList<>();
            while (elements.next()) list.add(readValue(elements, 2, elementType));
            return list;
        }
    }

    static ErrorCode errorCode(String sqlState) {
        PSQLState state = Arrays.stream(PSQLState.values()).filter(s -> s.getState().equals(sqlState)).findFirst().orElse(PSQLState.UNKNOWN_STATE);
        return switch (state) {
            case UNIQUE_VIOLATION -> ErrorCode.UNIQUE_VIOLATION;
            case FOREIGN_KEY_VIOLATION -> ErrorCode.FOREIGN_KEY_VIOLATION;
            case CHECK_VIOLATION -> ErrorCode.CHECK_VIOLATION;
            case NOT_NULL_VIOLATION -> ErrorCode.NOT_NULL_VIOLATION;
            case STRING_DATA_RIGHT_TRUNCATION, NUMERIC_VALUE_OUT_OF_RANGE, DATETIME_OVERFLOW -> ErrorCode.VALUE_OUT_OF_RANGE;
            case DATATYPE_MISMATCH -> ErrorCode.INVALID_VALUE;
            default -> isDataException(sqlState) ? ErrorCode.INVALID_VALUE : ErrorCode.DATABASE_ERROR;
        };
    }

    private static boolean isDataException(String sqlState) {
        String dataClass = PSQLState.DATA_ERROR.getState().substring(0, 2);
        return sqlState != null && sqlState.startsWith(dataClass);
    }
}
