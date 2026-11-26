package rowsmith.internal.schema;

import java.util.Set;
import rowsmith.internal.sql.SqlExpr;
import rowsmith.internal.sql.SqlParser;

public final class ColumnDef {
    public final String name;
    public final int num;
    public final PgType type;
    public final int typmod;
    public final String formattedType;
    public final boolean notNull;
    public final String defaultExpr;
    public final SqlExpr defaultParsed;
    public final boolean stableDefault;
    public final Identity identity;
    public final boolean generated;
    public final String sequence;
    TableDef table;

    public enum Identity {
        NONE, ALWAYS, BY_DEFAULT;

        public static Identity of(String attidentity) {
            return switch (attidentity == null ? "" : attidentity) {
                case "a" -> ALWAYS;
                case "d" -> BY_DEFAULT;
                default -> NONE;
            };
        }
    }

    public ColumnDef(String name, int num, PgType type, int typmod, String formattedType, boolean notNull,
                  String defaultExpr, Identity identity, boolean generated, String sequence, Set<String> volatileFunctions) {
        this.name = name;
        this.num = num;
        this.type = type;
        this.typmod = typmod;
        this.formattedType = formattedType;
        this.notNull = notNull;
        this.defaultExpr = defaultExpr;
        this.defaultParsed = defaultExpr == null ? null : SqlParser.tryParse(defaultExpr);
        this.stableDefault = defaultParsed != null && SqlExpr.functionsOf(defaultParsed).stream().noneMatch(volatileFunctions::contains);
        this.identity = identity;
        this.generated = generated;
        this.sequence = sequence;
    }

    public TableDef table() { return table; }

    public boolean isIdentity() { return identity != Identity.NONE; }
    public boolean isGenerated() { return generated; }
    public boolean drawsFromSequence() { return sequence != null; }
    public boolean hasDefault() { return defaultExpr != null || isIdentity(); }

    public boolean required() {
        return notNull || type.domainChain().anyMatch(t -> t.domainNotNull);
    }

    public int effectiveTypmod() {
        if (typmod >= 0) return typmod;
        return type.domainChain().mapToInt(t -> t.baseTypmod).filter(m -> m >= 0).findFirst().orElse(-1);
    }

    public PgType baseType() {
        return type.baseType();
    }

    public String qualifiedName() { return table.displayName() + "." + name; }

    @Override
    public String toString() { return qualifiedName() + " " + formattedType; }
}
