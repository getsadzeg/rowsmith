package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class TrickySchemaTest {

    private static final Pattern EXPECT = Pattern.compile("^-- expect (\\S+): (\\w+)", Pattern.MULTILINE);

    @TestFactory
    Stream<DynamicNode> everyTableSeedsOrFailsClearly() throws IOException, URISyntaxException {
        Path dir = Path.of(getClass().getClassLoader().getResource("tricky").toURI());
        List<DynamicNode> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.sorted().toList()) {
                String sql = Files.readString(p);
                Map<String, ErrorCode> expected = new HashMap<>();
                Matcher m = EXPECT.matcher(sql);
                while (m.find()) expected.put(m.group(1), ErrorCode.valueOf(m.group(2)));
                files.add(DynamicContainer.dynamicContainer(p.getFileName().toString(), tablesOf(sql, expected)));
            }
        }
        return files.stream();
    }

    private Stream<DynamicNode> tablesOf(String sql, Map<String, ErrorCode> expected) {
        DataSource ds = Pg.withSchema(sql);
        Rowsmith probe = Rowsmith.builder(ds).build();
        List<DynamicNode> tests = new ArrayList<>();
        for (String table : probe.tables()) {
            ErrorCode code = expected.get(table);
            tests.add(DynamicTest.dynamicTest(table + (code == null ? " seeds" : " fails with " + code), () -> {
                Rowsmith rs = Rowsmith.of(ds);
                if (code == null) {
                    for (int i = 0; i < 3; i++) assertThat(rs.create(table)).isNotNull();
                    assertThat(rs.createMany(table, 3)).hasSize(3);
                    assertThat(rs.plan(table).sql()).isNotEmpty();
                } else {
                    assertThatThrownBy(() -> rs.create(table))
                            .isInstanceOfSatisfying(RowsmithException.class, e -> {
                                assertThat(e.code()).isEqualTo(code);
                                assertThat(e.getMessage()).startsWith("Rowsmith: ").contains(table);
                            });
                }
            }));
        }
        return tests.stream();
    }
}
