package io.github.rowsmith.internal.gen;

import io.github.rowsmith.internal.schema.PgType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public enum TypeKind {
    INT2(Integer.class), INT4(Integer.class), INT8(Long.class), NUMERIC(BigDecimal.class), FLOAT(Double.class),
    BOOL(Boolean.class), TEXT(String.class), CHAR(String.class), UUID(java.util.UUID.class), DATE(LocalDate.class),
    TIMESTAMP(LocalDateTime.class), TIMESTAMPTZ(OffsetDateTime.class), TIME(LocalTime.class), TIMETZ(String.class),
    INTERVAL(String.class), JSON(String.class), BYTEA(byte[].class), INET(String.class), CIDR(String.class),
    MACADDR(String.class), MONEY(String.class), TSVECTOR(String.class), XML(String.class), ENUM(String.class),
    ARRAY(List.class), BIT(String.class), VECTOR(String.class), UNSUPPORTED(String.class);

    private final Class<?> javaType;

    TypeKind(Class<?> javaType) {
        this.javaType = javaType;
    }

    public Class<?> javaType() {
        return javaType;
    }

    public static PgType strip(PgType t) {
        return t == null ? null : t.baseType();
    }

    public static TypeKind of(PgType type) {
        PgType t = strip(type);
        if (t == null) return UNSUPPORTED;
        if (t.isEnum()) return ENUM;
        if (t.isArray()) return ARRAY;
        if (t.schema.equals("pg_catalog") && ofCatalogName(t.name) != UNSUPPORTED) return ofCatalogName(t.name);
        if (t.name.equals("citext")) return TEXT;
        if (t.name.equals("vector")) return VECTOR;
        return switch (t.category) {
            case STRING -> TEXT;
            case NUMERIC -> NUMERIC;
            case BOOLEAN -> BOOL;
            default -> UNSUPPORTED;
        };
    }

    public static TypeKind ofCatalogName(String name) {
        return switch (name) {
            case "int2" -> INT2;
            case "int4" -> INT4;
            case "int8" -> INT8;
            case "numeric" -> NUMERIC;
            case "float4", "float8" -> FLOAT;
            case "bool" -> BOOL;
            case "text", "varchar", "name" -> TEXT;
            case "bpchar", "char" -> CHAR;
            case "uuid" -> UUID;
            case "date" -> DATE;
            case "timestamp" -> TIMESTAMP;
            case "timestamptz" -> TIMESTAMPTZ;
            case "time" -> TIME;
            case "timetz" -> TIMETZ;
            case "interval" -> INTERVAL;
            case "json", "jsonb" -> JSON;
            case "bytea" -> BYTEA;
            case "inet" -> INET;
            case "cidr" -> CIDR;
            case "macaddr", "macaddr8" -> MACADDR;
            case "money" -> MONEY;
            case "tsvector" -> TSVECTOR;
            case "xml" -> XML;
            case "bit", "varbit" -> BIT;
            default -> UNSUPPORTED;
        };
    }

    public Object read(ResultSet rs, int i) throws SQLException {
        if (this == FLOAT) {
            double d = rs.getDouble(i);
            return rs.wasNull() ? null : d;
        }
        return javaType == String.class ? rs.getString(i) : rs.getObject(i, javaType);
    }
}
