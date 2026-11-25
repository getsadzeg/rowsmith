package io.github.rowsmith;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class RealWorldCheckTest {

    enum Outcome { OK, CLEAR_ERROR, BAD }

    record Case(String file, String id, String source, String license, Outcome expected, int minPg, String sql) {}

    record Result(Case c, String table, Outcome outcome, String checks, String detail) {}

    static final int ROWS = 20;
    private static final Pattern HEADER = Pattern.compile("^-- case: (\\S+)\\R-- source: (\\S+)\\R-- license: (\\S+)\\R(?:-- expect: (\\w+)\\R)?(?:-- requires: PostgreSQL (\\d+)\\R)?", Pattern.MULTILINE);
    private static final Pattern SET_HINT = Pattern.compile("; set ([\\w$]+(?:, [\\w$]+)*) with \\.set\\(\\.\\.\\.\\) in the test or with RowsmithSetup\\.value\\(\\.\\.\\.\\)");
    private static final Set<ErrorCode> CLEAR_CODES = Set.of(ErrorCode.CANNOT_GENERATE, ErrorCode.CHECK_VIOLATION, ErrorCode.UNSUPPORTED_TYPE);
    static final List<Result> RESULTS = new ArrayList<>();

    @TestFactory
    Stream<DynamicNode> everyCheckSeedsOrFailsClearly() throws IOException, URISyntaxException {
        Path dir = Path.of(getClass().getClassLoader().getResource("checks").toURI());
        List<DynamicNode> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.filter(f -> f.toString().endsWith(".sql")).sorted().toList()) {
                List<Case> cases = parse(p.getFileName().toString(), Files.readString(p));
                files.add(DynamicContainer.dynamicContainer(p.getFileName().toString(), casesOf(cases)));
            }
        }
        return files.stream();
    }

    static List<Case> parse(String file, String text) {
        List<Case> out = new ArrayList<>();
        Matcher m = HEADER.matcher(text);
        List<int[]> spans = new ArrayList<>();
        List<String[]> heads = new ArrayList<>();
        while (m.find()) {
            spans.add(new int[] {m.start(), m.end()});
            heads.add(new String[] {m.group(1), m.group(2), m.group(3), m.group(4) == null ? "OK" : m.group(4), m.group(5) == null ? "0" : m.group(5)});
        }
        for (int i = 0; i < spans.size(); i++) {
            int end = i + 1 < spans.size() ? spans.get(i + 1)[0] : text.length();
            String[] h = heads.get(i);
            out.add(new Case(file, h[0], h[1], h[2], Outcome.valueOf(h[3]), Integer.parseInt(h[4]), text.substring(spans.get(i)[1], end).strip()));
        }
        assertThat(out).as("entries in %s", file).isNotEmpty();
        return out;
    }

    private static Map<String, String> loadEachCaseIntoItsOwnSchema(DataSource ds, List<Case> cases) {
        Map<String, String> schemaOf = new TreeMap<>();
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            String schema = "c" + (i + 1);
            Pg.exec(ds, "CREATE SCHEMA " + schema + "; SET search_path = " + schema + ", public;\n" + c.sql());
            schemaOf.put(c.id(), schema);
        }
        return schemaOf;
    }

    private Stream<DynamicNode> casesOf(List<Case> cases) {
        DataSource ds = Pg.freshDatabase();
        Map<String, String> schemaOf = loadEachCaseIntoItsOwnSchema(ds, cases);
        List<DynamicNode> tests = new ArrayList<>();
        for (Case c : cases) {
            String schema = schemaOf.get(c.id());
            tests.add(DynamicTest.dynamicTest(c.id(), () -> {
                Assumptions.assumeTrue(Pg.serverMajorVersion(ds) >= c.minPg(), "needs PostgreSQL " + c.minPg());
                for (String table : tablesIn(ds, schema)) {
                    Result r = run(ds, c, schema, table);
                    synchronized (RESULTS) { RESULTS.add(r); }
                    assertThat(r.outcome()).as("%s (%s): %s", c.id(), r.checks(), r.detail()).isEqualTo(c.expected());
                    if (c.expected() == Outcome.CLEAR_ERROR) {
                        assertThat(columnsToSet(r.detail())).as("columns to set in: %s", r.detail()).isNotEmpty().isSubsetOf(columns(ds, schema, table));
                    }
                }
            }));
        }
        return tests.stream();
    }

    private static List<String> tablesIn(DataSource ds, String schema) {
        String csv = Pg.queryString(ds, "SELECT string_agg(relname, ',' ORDER BY relname) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = '" + schema + "' AND c.relkind IN ('r', 'p')");
        return csv == null ? List.of() : List.of(csv.split(","));
    }

    static Result run(DataSource ds, Case c, String schema, String table) {
        String qualified = schema + "." + table;
        String checks = Pg.queryString(ds, "SELECT string_agg(pg_get_constraintdef(oid), ' ; ') FROM pg_constraint WHERE conrelid = '"
                + schema + ".\"" + table + "\"'::regclass AND contype = 'c'");
        Rowsmith rs = Rowsmith.of(ds);
        try {
            List<Row> rows = rs.createMany(qualified, ROWS);
            long count = Pg.count(ds, schema + ".\"" + table + "\"");
            if (rows.size() != ROWS || count < ROWS) {
                return new Result(c, table, Outcome.BAD, checks, "asked for " + ROWS + " rows, got " + rows.size() + " (table has " + count + ")");
            }
            return new Result(c, table, Outcome.OK, checks, "");
        } catch (RowsmithException e) {
            return new Result(c, table, classify(e, ds, schema, table), checks, e.code() + " " + e.getMessage());
        } catch (RuntimeException | Error e) {
            return new Result(c, table, Outcome.BAD, checks, e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private static List<String> columnsToSet(String message) {
        Matcher m = SET_HINT.matcher(message);
        return m.find() ? List.of(m.group(1).split(", ")) : List.of();
    }

    private static List<String> columns(DataSource ds, String schema, String table) {
        return List.of(Pg.queryString(ds, "SELECT string_agg(attname, ',') FROM pg_attribute WHERE attrelid = '" + schema + ".\"" + table
                + "\"'::regclass AND attnum > 0 AND NOT attisdropped").split(","));
    }

    private static Outcome classify(RowsmithException e, DataSource ds, String schema, String table) {
        for (Throwable t = e; t != null; t = t.getCause()) if (t instanceof SQLException) return Outcome.BAD;
        if (!CLEAR_CODES.contains(e.code())) return Outcome.BAD;
        String msg = e.getMessage().toLowerCase(Locale.ROOT);
        if (!msg.contains("set")) return Outcome.BAD;
        for (String col : columns(ds, schema, table)) if (msg.contains(col.toLowerCase(Locale.ROOT))) return Outcome.CLEAR_ERROR;
        return Outcome.BAD;
    }

    @AfterAll
    static void report() throws IOException {
        Map<String, Map<Outcome, Integer>> bySource = new TreeMap<>();
        Map<Outcome, Integer> total = new EnumMap<>(Outcome.class);
        for (Result r : RESULTS) {
            String src = r.c().file().replace(".sql", "");
            bySource.computeIfAbsent(src, k -> new EnumMap<>(Outcome.class)).merge(r.outcome(), 1, Integer::sum);
            total.merge(r.outcome(), 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder("# Real-world CHECK constraints\n\n");
        sb.append("Each entry: create ").append(ROWS).append(" rows with no configuration. Postgres image: ").append(Pg.image()).append("\n\n");
        sb.append("| File | Tables | OK | CLEAR_ERROR | BAD |\n|---|---:|---:|---:|---:|\n");
        bySource.forEach((src, n) -> sb.append(tableRow(src, n)));
        sb.append(tableRow("**total**", total));
        for (Outcome o : List.of(Outcome.BAD, Outcome.CLEAR_ERROR)) {
            List<Result> list = RESULTS.stream().filter(r -> r.outcome() == o).toList();
            if (list.isEmpty()) continue;
            sb.append("\n## ").append(o).append(" (").append(list.size()).append(")\n");
            for (Result r : list) {
                sb.append("\n- `").append(r.c().id()).append("` ").append(r.table()).append(": `").append(r.checks()).append("`\n  ")
                        .append(r.detail().replace("\n", " ")).append('\n');
            }
        }
        String dir = System.getProperty("rowsmith.test.reportDir", "build/reports/rowsmith");
        Files.createDirectories(Path.of(dir));
        Files.writeString(Path.of(dir, "checks.md"), sb.toString());
        System.out.println("checks: OK " + count(total, Outcome.OK) + ", CLEAR_ERROR " + count(total, Outcome.CLEAR_ERROR) + ", BAD " + count(total, Outcome.BAD));
    }

    private static String tableRow(String label, Map<Outcome, Integer> n) {
        int ok = count(n, Outcome.OK);
        int clear = count(n, Outcome.CLEAR_ERROR);
        int bad = count(n, Outcome.BAD);
        return "| " + label + " | " + (ok + clear + bad) + " | " + ok + " | " + clear + " | " + bad + " |\n";
    }

    private static int count(Map<Outcome, Integer> n, Outcome o) {
        return n.getOrDefault(o, 0);
    }
}
