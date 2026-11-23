package io.github.rowsmith.internal.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class PgType {
    public final long oid;
    public final String schema;
    public final String name;
    public final String castName;
    public final Kind kind;
    public final Category category;
    public final long elemOid;
    public final long baseOid;
    public final int baseTypmod;
    public final boolean domainNotNull;
    public final List<String> enumLabels = new ArrayList<>();
    public final List<CheckConstraint> domainChecks = new ArrayList<>();
    PgType element;
    PgType base;

    public enum Kind {
        BASE, COMPOSITE, DOMAIN, ENUM, PSEUDO, RANGE, MULTIRANGE;

        static Kind of(String typtype) {
            return switch (typtype) {
                case "b" -> BASE;
                case "c" -> COMPOSITE;
                case "d" -> DOMAIN;
                case "e" -> ENUM;
                case "p" -> PSEUDO;
                case "r" -> RANGE;
                case "m" -> MULTIRANGE;
                default -> throw new IllegalArgumentException("pg_type.typtype " + typtype);
            };
        }
    }

    public enum Category {
        ARRAY, BOOLEAN, COMPOSITE, DATETIME, ENUM, GEOMETRIC, NETWORK, NUMERIC, PSEUDO, RANGE, STRING, TIMESPAN,
        USER, BITSTRING, UNKNOWN, INTERNAL;

        static Category of(String typcategory) {
            return switch (typcategory) {
                case "A" -> ARRAY;
                case "B" -> BOOLEAN;
                case "C" -> COMPOSITE;
                case "D" -> DATETIME;
                case "E" -> ENUM;
                case "G" -> GEOMETRIC;
                case "I" -> NETWORK;
                case "N" -> NUMERIC;
                case "P" -> PSEUDO;
                case "R" -> RANGE;
                case "S" -> STRING;
                case "T" -> TIMESPAN;
                case "U" -> USER;
                case "V" -> BITSTRING;
                case "X" -> UNKNOWN;
                case "Z" -> INTERNAL;
                default -> throw new IllegalArgumentException("pg_type.typcategory " + typcategory);
            };
        }
    }

    PgType(long oid, String schema, String name, String castName, Kind kind, Category category,
                  long elemOid, long baseOid, int baseTypmod, boolean domainNotNull) {
        this.oid = oid;
        this.schema = schema;
        this.name = name;
        this.castName = castName;
        this.kind = kind;
        this.category = category;
        this.elemOid = elemOid;
        this.baseOid = baseOid;
        this.baseTypmod = baseTypmod;
        this.domainNotNull = domainNotNull;
    }

    public boolean isDomain() { return kind == Kind.DOMAIN; }
    public boolean isEnum() { return kind == Kind.ENUM; }
    public boolean isArray() { return category == Category.ARRAY && element != null; }
    public PgType element() { return element; }
    public PgType base() { return base; }

    public Stream<PgType> domainChain() {
        return Stream.iterate(this, Objects::nonNull, t -> t.isDomain() ? t.base : null);
    }

    public PgType baseType() {
        return domainChain().reduce((outer, inner) -> inner).orElseThrow();
    }

    @Override
    public String toString() { return castName; }
}
