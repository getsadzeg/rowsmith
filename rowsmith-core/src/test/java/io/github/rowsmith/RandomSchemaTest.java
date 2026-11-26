package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RandomSchemaTest {

    record Generated(String ddl, List<String> tables, boolean cycle) {}

    static IntStream seeds() {
        return IntStream.rangeClosed(1, 60);
    }

    @ParameterizedTest
    @MethodSource("seeds")
    void randomSchemasSeedOrFailWithDefinedError(int schemaSeed) {
        Generated g = generate(new Random(schemaSeed));
        DataSource ds = Pg.withSchema(g.ddl());
        Rowsmith rs = Rowsmith.of(ds);
        for (String table : g.tables()) {
            try {
                rs.create(table);
                rs.createMany(table, 2);
            } catch (RowsmithException e) {
                assertThat(g.cycle()).as("unexpected failure for %s (seed %s) in schema:%n%s%n%s", table, schemaSeed, g.ddl(), e.getMessage()).isTrue();
                assertThat(e.code()).as(e.getMessage()).isEqualTo(ErrorCode.REQUIRED_FK_CYCLE);
            }
        }
    }

    static Generated generate(Random r) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TYPE color AS ENUM ('red', 'green', 'blue');\n");
        ddl.append("CREATE DOMAIN pos_amount AS numeric(10,2) CHECK (VALUE > 0);\n");
        ddl.append("CREATE DOMAIN code3 AS varchar(3) CHECK (VALUE ~ '^[A-Z]{3}$');\n");
        int n = 1 + r.nextInt(6);
        List<String> tables = new ArrayList<>();
        boolean cycle = false;
        for (int t = 0; t < n; t++) {
            String name = "t" + t;
            tables.add(name);
            List<String> cols = new ArrayList<>();
            List<String> constraints = new ArrayList<>();
            switch (r.nextInt(4)) {
                case 0 -> cols.add("id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY");
                case 1 -> cols.add("id serial PRIMARY KEY");
                case 2 -> cols.add("id uuid PRIMARY KEY DEFAULT gen_random_uuid()");
                default -> cols.add("id text PRIMARY KEY");
            }
            int ncols = 1 + r.nextInt(6);
            List<String> plain = new ArrayList<>();
            List<String> fewValues = new ArrayList<>();
            for (int c = 0; c < ncols; c++) {
                String col = "c" + c;
                String notNull = r.nextInt(3) > 0 ? " NOT NULL" : "";
                String unique = r.nextInt(5) == 0 ? " UNIQUE" : "";
                String def = switch (r.nextInt(14)) {
                    case 0 -> "int" + notNull + unique + (r.nextBoolean() ? " CHECK (" + col + " BETWEEN 1 AND 500)" : "");
                    case 1 -> "bigint" + notNull + unique;
                    case 2 -> "numeric(" + (6 + r.nextInt(6)) + ",2)" + notNull + (r.nextBoolean() ? " CHECK (" + col + " > 0)" : "");
                    case 3 -> "text" + notNull + unique + (unique.isEmpty() && r.nextBoolean() ? " CHECK (" + col + " IN ('A', 'B', 'C'))" : "");
                    case 4 -> "varchar(" + (3 + r.nextInt(30)) + ")" + notNull + unique;
                    case 5 -> "char(" + (1 + r.nextInt(4)) + ")" + notNull;
                    case 6 -> "boolean" + notNull;
                    case 7 -> "date" + notNull + (r.nextBoolean() ? " CHECK (" + col + " >= DATE '2000-01-01')" : "");
                    case 8 -> "timestamptz" + notNull;
                    case 9 -> "uuid" + notNull + unique;
                    case 10 -> "jsonb" + notNull;
                    case 11 -> "color" + notNull;
                    case 12 -> "text[]" + notNull;
                    default -> (r.nextBoolean() ? "pos_amount" : "code3") + notNull;
                };
                cols.add(col + " " + def);
                plain.add(col);
                if (hasFewPossibleValues(def)) fewValues.add(col);
            }
            if (plain.size() >= 2 && r.nextInt(3) == 0 && (!fewValues.contains(plain.get(0)) || !fewValues.contains(plain.get(1)))) {
                constraints.add("UNIQUE (" + plain.get(0) + ", " + plain.get(1) + ")");
            }
            if (t > 0) {
                int fks = r.nextInt(Math.min(t, 3) + 1);
                for (int f = 0; f < fks; f++) {
                    int target = r.nextInt(t);
                    String fkType = refType(ddl, "t" + target);
                    boolean required = r.nextBoolean();
                    cols.add("fk" + f + " " + fkType + (required ? " NOT NULL" : "") + " REFERENCES t" + target + "(id)");
                    if (r.nextInt(6) == 0) constraints.add("UNIQUE (fk" + f + ")");
                }
            }
            if (r.nextInt(5) == 0) {
                String selfType = cols.get(0).split(" ")[1];
                cols.add("parent_id " + selfType + " REFERENCES " + name + "(id)");
            }
            List<String> all = new ArrayList<>(cols);
            all.addAll(constraints);
            ddl.append("CREATE TABLE ").append(name).append(" (\n  ").append(String.join(",\n  ", all)).append("\n);\n");
        }
        if (n >= 2 && r.nextInt(6) == 0) {
            String last = "t" + (n - 1);
            ddl.append("ALTER TABLE t0 ADD COLUMN back_id ").append(refType(ddl, last)).append(" NOT NULL REFERENCES ").append(last).append("(id);\n");
            cycle = true;
        }
        return new Generated(ddl.toString(), tables, cycle);
    }

    private static boolean hasFewPossibleValues(String columnDef) {
        return columnDef.startsWith("boolean") || columnDef.startsWith("color") || columnDef.startsWith("char(") || columnDef.contains(" IN (");
    }

    private static String refType(StringBuilder ddl, String table) {
        String text = ddl.toString();
        int i = text.indexOf("CREATE TABLE " + table + " (");
        String idLine = text.substring(text.indexOf("id ", i) + 3);
        String type = idLine.substring(0, idLine.indexOf(' '));
        return switch (type) {
            case "serial" -> "int";
            default -> type;
        };
    }
}
