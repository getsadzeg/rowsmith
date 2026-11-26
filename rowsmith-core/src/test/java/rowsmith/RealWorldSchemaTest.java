package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class RealWorldSchemaTest {

    record Result(String schema, int tables, int ok, List<String> failures) {}

    static final Map<String, Result> RESULTS = new LinkedHashMap<>();

    static Result seedAll(String name, DataSource ds) {
        return seedAll(name, ds, new RowsmithSetup.None());
    }

    static Result seedAll(String name, DataSource ds, RowsmithSetup setup) {
        Rowsmith probe = Rowsmith.builder(ds).build();
        List<String> failures = new ArrayList<>();
        int ok = 0;
        List<String> tables = probe.tables();
        for (String table : tables) {
            Rowsmith rs = Rowsmith.builder(ds).setup(setup).build();
            try {
                rs.create(table);
                rs.createMany(table, 3);
                ok++;
            } catch (RowsmithException e) {
                failures.add(table + ": " + e.code() + " " + e.getMessage());
            }
        }
        Result r = new Result(name, tables.size(), ok, failures);
        RESULTS.put(name, r);
        return r;
    }

    static DataSource load(DataSource ds, String resource) {
        try (var in = RealWorldSchemaTest.class.getClassLoader().getResourceAsStream(resource)) {
            Pg.exec(ds, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return ds;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void pagila() {
        String image = "pgvector/pgvector:pg18";
        DataSource ds = load(Pg.freshDatabaseOn(image), "realworld/pagila.sql");
        Pg.exec(ds, """
            ALTER TABLE payment ADD FOREIGN KEY (customer_id) REFERENCES customer (customer_id);
            ALTER TABLE payment ADD FOREIGN KEY (rental_id) REFERENCES rental (rental_id);
            ALTER TABLE payment ADD FOREIGN KEY (staff_id) REFERENCES staff (staff_id);
            """);
        Result r = seedAll("Pagila", ds, c -> c.value("payment", "payment_date", n -> OffsetDateTime.parse("2022-03-01T12:00:00Z").plusMinutes(n)));
        assertThat(r.failures()).isEmpty();
    }

    @Test
    void chinook() {
        Result r = seedAll("Chinook", load(Pg.freshDatabase(), "realworld/chinook.sql"));
        assertThat(r.failures()).isEmpty();
    }

    @Test
    void petclinic() {
        Result r = seedAll("Spring PetClinic", load(Pg.freshDatabase(), "realworld/petclinic.sql"));
        assertThat(r.failures()).isEmpty();
    }

    @Test
    void keycloak() {
        var url = getClass().getClassLoader().getResource("realworld/keycloak.sql");
        Assumptions.assumeTrue(url != null, "keycloak.sql not present");
        Result r = seedAll("Keycloak", load(Pg.freshDatabase(), "realworld/keycloak.sql"));
        assertThat(r.failures()).isEmpty();
    }

    @AfterAll
    static void report() throws IOException {
        StringBuilder sb = new StringBuilder("| Schema | Tables | Seedable | Coverage |\n|---|---:|---:|---:|\n");
        for (Result r : RESULTS.values()) {
            sb.append("| ").append(r.schema()).append(" | ").append(r.tables()).append(" | ").append(r.ok()).append(" | ")
                    .append(String.format("%.0f%%", 100.0 * r.ok() / Math.max(1, r.tables()))).append(" |\n");
        }
        sb.append("\nPostgres image: ").append(Pg.image()).append("\n");
        for (Result r : RESULTS.values()) {
            for (String f : r.failures()) sb.append("\n- ").append(r.schema()).append(" / ").append(f);
        }
        System.out.println(sb);
        String dir = System.getProperty("rowsmith.test.reportDir", "build/reports/rowsmith");
        Files.createDirectories(Path.of(dir));
        Files.writeString(Path.of(dir, "realworld.md"), sb.toString());
    }
}
