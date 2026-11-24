package io.github.rowsmith.codegen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/**
 * Runs SQL scripts in the given order, each as one batch of statements (the server parses them, so
 * functions, DO blocks and dollar quoting work as in psql without meta-commands).
 */
public record SqlScriptsSource(List<Path> scripts) implements SchemaSource {

    public SqlScriptsSource {
        scripts = List.copyOf(scripts);
    }

    @Override
    public void apply(DataSource dataSource) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            for (Path script : scripts) st.execute(Files.readString(script));
        }
    }
}
