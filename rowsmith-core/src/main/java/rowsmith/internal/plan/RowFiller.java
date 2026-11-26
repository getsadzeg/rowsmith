package rowsmith.internal.plan;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import rowsmith.ErrorCode;
import rowsmith.RelativeTime;
import rowsmith.Row;
import rowsmith.RowsmithException;
import rowsmith.ValueSource;
import rowsmith.internal.gen.Coercer;
import rowsmith.internal.gen.TypeKind;
import rowsmith.internal.gen.ValueGenerator;
import rowsmith.internal.schema.ColumnDef;
import rowsmith.internal.schema.ForeignKey;
import rowsmith.internal.schema.Schema;
import rowsmith.internal.schema.UniqueKey;
import rowsmith.internal.sql.ColumnRules;

final class RowFiller {

    private static final Set<ValueSource> UNSET = EnumSet.of(ValueSource.ROWSMITH, ValueSource.LEFT_NULL);

    private final Engine engine;
    private final PgEval db;
    private final GenState state;
    private final KeyRegistry keys;
    private final PlanErrors errors;

    RowFiller(Engine engine, PgEval db, GenState state, KeyRegistry keys, PlanErrors errors) {
        this.engine = engine;
        this.db = db;
        this.state = state;
        this.keys = keys;
        this.errors = errors;
    }

    void applyValue(Node n, String colName, Object value, ValueSource source) {
        ColumnDef c = Schema.column(n.table, colName);
        String from = source == ValueSource.SETUP ? " (" + PlanErrors.setupValue(c) + ")" : "";
        if (c.isGenerated()) {
            throw new RowsmithException(ErrorCode.INVALID_VALUE, c.qualifiedName() + " is a GENERATED column; the database computes it, it cannot be set");
        }
        if (value instanceof RelativeTime rt) value = engine.api.expr(rt);
        if (value instanceof Row r) {
            useRow(n, c, r);
            n.sources.put(c, source);
            return;
        }
        if (value == null) {
            if (c.required()) {
                throw new RowsmithException(ErrorCode.NOT_NULL_VIOLATION, c.qualifiedName() + " is NOT NULL and cannot be set to null" + from);
            }
            n.assign(c, new Assignment.Null());
            n.sources.put(c, source);
            return;
        }
        Object v = source == ValueSource.SETUP ? coerceSetupValue(c, value) : typed(c, value);
        n.assign(c, new Assignment.Value(v));
        n.sources.put(c, source);
    }

    static void useRow(Node n, ColumnDef c, Row r) {
        List<ForeignKey> fks = n.table.foreignKeys.stream().filter(fk -> fk.columns.contains(c)).toList();
        if (fks.isEmpty()) {
            throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, "use(\"" + c.name + "\", row): " + c.qualifiedName() + " is not a foreign key column");
        }
        for (ForeignKey fk : fks) {
            if (!r.qualifiedTable().equals(fk.refTable().qualifiedName())) {
                throw new RowsmithException(ErrorCode.INVALID_RELATIONSHIP, "use(\"" + c.name + "\", row): " + n.table.displayName() + "." + c.name
                        + " references " + fk.refTable().displayName() + ", but the row is a " + r.table());
            }
            n.link(fk, new Ref.Existing(r));
        }
    }

    private Object coerceSetupValue(ColumnDef c, Object value) {
        try {
            return typed(c, value);
        } catch (RowsmithException e) {
            throw new RowsmithException(e.code(), e.detail() + " (" + PlanErrors.setupValue(c) + ")", e);
        }
    }

    Object typed(ColumnDef c, Object value) {
        Object v = Coercer.coerce(c, value, engine.clock());
        return v instanceof String s && TypeKind.of(c.type).javaType() != String.class ? db.read(s, c) : v;
    }

    void generate(Node n) {
        String table = n.table.qualifiedName();
        n.number = state.nextNumber(table);
        fill(n);
        long skips = keys.takenTuples(n);
        UniqueKey taken = keys.takenKey(n);
        while (taken != null && skips-- > 0) {
            renumber(n, state.nextNumber(table));
            taken = keys.takenKey(n);
        }
        if (taken != null) throw errors.repeats(n, taken);
        keys.checkKeys(n, false);
        keys.register(n, false);
    }

    void renumber(Node n, long number) {
        for (ColumnDef c : n.table.columns()) if (n.isNumbered(c)) n.assign(c, new Assignment.Default());
        n.number = number;
        fill(n);
    }

    void keepNumber(Node n, long number) {
        if (n.number != number) renumber(n, number);
        state.numberUsed(n.table.qualifiedName(), number);
    }

    private void fill(Node n) {
        for (ColumnDef c : n.table.columns()) {
            ValueSource source = classify(n, c);
            if (UNSET.contains(source) && n.table.partitioning != null && n.table.partitioning.needsKey(c)) throw errors.partitionKey(n, unsetKeys(n));
            n.sources.put(c, source);
            switch (source) {
                case SETUP -> applyValue(n, c.name, engine.pattern(c).apply(Math.toIntExact(n.number)), ValueSource.SETUP);
                case ROWSMITH -> n.assign(c, new Assignment.Value(gen(n, c, n.rulesFor(c))));
                default -> { }
            }
        }
    }

    private List<String> unsetKeys(Node n) {
        return n.table.partitioning.keyColumns().stream().filter(k -> UNSET.contains(classify(n, k))).map(k -> k.name).toList();
    }

    private ValueSource classify(Node n, ColumnDef c) {
        if (n.sources.get(c) == ValueSource.USER) return ValueSource.USER;
        if (c.isGenerated()) return ValueSource.DATABASE_GENERATED;
        if (engine.pattern(c) != null) return ValueSource.SETUP;
        if (n.model.fkColumns.contains(c)) return ValueSource.PARENT_ROW;
        if (c.isIdentity() || c.drawsFromSequence()) return ValueSource.DATABASE_GENERATED;
        if (n.inheritsRules(c)) return ValueSource.ROWSMITH;
        if (c.hasDefault() && !n.model.generateOverDefault(c)) return ValueSource.DATABASE_DEFAULT;
        if (n.model.leaveNull(c)) return ValueSource.LEFT_NULL;
        return ValueSource.ROWSMITH;
    }

    void regenerate(Node n, ColumnDef c, ColumnRules extra) {
        n.assign(c, new Assignment.Value(gen(n, c, n.rulesFor(c).and(extra))));
    }

    private Object gen(Node n, ColumnDef c, ColumnRules rules) {
        return engine.generator().generate(c, new ValueGenerator.Request(rules, n.number, n.model.uniqueness(c)));
    }
}
