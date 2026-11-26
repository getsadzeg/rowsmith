package rowsmith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rowsmith.Row;
import rowsmith.Rowsmith;
import rowsmith.RowsmithException;

class SupportMatrixTest {

    static final String SCENARIO = "rowsmith.supportMatrix.scenario";
    static final String DATABASE = "support_matrix";
    static final String REFUSED = "refused";
    static final String REFUSAL = "refused by the database";
    static final int APP_ROWS = 1;
    static final int MISSING_ID = 424_242;
    static final List<String> TABLES = List.of("merchant", "store", "a", "b");
    static final DataSource DATA_SOURCE = database();

    enum Scenario {
        PLAIN_FOREIGN_KEYS("store", "INSERT INTO store (merchant_id) VALUES (?)"),
        DEFERRED_CYCLE("a", "INSERT INTO b (a_id) VALUES (?)");

        final String root;
        final String danglingInsert;

        Scenario(String root, String danglingInsert) {
            this.root = root;
            this.danglingInsert = danglingInsert;
        }
    }

    static Stream<Arguments> combinations() {
        return Stream.of(InSpringTransaction.class, InSpringWithoutTransaction.class, WithoutTransaction.class)
                .flatMap(mode -> Arrays.stream(Scenario.values()).map(scenario -> Arguments.of(Named.of(mode.getSimpleName(), mode), scenario)));
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("combinations")
    void everyLifecycleSeesItsRowsAndLeavesTheDatabaseAsItFoundIt(Class<? extends Mode> mode, Scenario scenario) {
        Map<String, String> before = snapshot();
        EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(mode))
                .configurationParameter(SCENARIO, scenario.name())
                .execute();
        assertThat(failures(results)).isEmpty();
        assertThat(results.testEvents().succeeded().count()).isEqualTo(expectedTests(mode));
        assertThat(snapshot()).isEqualTo(before);
    }

    private static List<Throwable> failures(EngineExecutionResults results) {
        return results.allEvents().failed().stream()
                .flatMap(e -> e.getRequiredPayload(TestExecutionResult.class).getThrowable().stream())
                .toList();
    }

    private static long expectedTests(Class<? extends Mode> mode) {
        long lifecycles = ReflectionSupport.findNestedClasses(mode, c -> c.isAnnotationPresent(Nested.class)).size();
        return lifecycles * AnnotationSupport.findAnnotatedMethods(Exercise.class, Test.class, HierarchyTraversalMode.TOP_DOWN).size();
    }

    private static Map<String, String> snapshot() {
        JdbcTemplate jdbc = new JdbcTemplate(DATA_SOURCE);
        Map<String, String> rows = new LinkedHashMap<>();
        for (String table : TABLES) {
            rows.put(table, jdbc.queryForObject("SELECT coalesce(string_agg(t::text, ',' ORDER BY t::text), '') FROM " + table + " t", String.class));
        }
        return rows;
    }

    private static DataSource database() {
        new JdbcTemplate(dataSource(ContainerFieldTest.PG.getDatabaseName())).execute("CREATE DATABASE " + DATABASE);
        DataSource ds = dataSource(DATABASE);
        new JdbcTemplate(ds).execute("""
                CREATE TABLE merchant (id serial PRIMARY KEY, name text NOT NULL);
                CREATE TABLE store (id serial PRIMARY KEY, merchant_id int NOT NULL REFERENCES merchant, name text NOT NULL DEFAULT 'store');
                CREATE TABLE a (id serial PRIMARY KEY, b_id int NOT NULL, name text NOT NULL DEFAULT 'a');
                CREATE TABLE b (id serial PRIMARY KEY, a_id int NOT NULL REFERENCES a DEFERRABLE INITIALLY IMMEDIATE);
                ALTER TABLE a ADD FOREIGN KEY (b_id) REFERENCES b DEFERRABLE INITIALLY IMMEDIATE;
                CREATE FUNCTION refuse() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'refused by the database'; END $$;
                CREATE TRIGGER refuse BEFORE INSERT ON store FOR EACH ROW WHEN (NEW.name = 'refused') EXECUTE FUNCTION refuse();
                CREATE TRIGGER refuse BEFORE INSERT ON a FOR EACH ROW WHEN (NEW.name = 'refused') EXECUTE FUNCTION refuse();
                INSERT INTO merchant (name) VALUES ('app');
                INSERT INTO store (merchant_id, name) VALUES (currval('merchant_id_seq'), 'app');
                WITH id AS (SELECT nextval('a_id_seq') AS a_id), b_row AS (INSERT INTO b (a_id) SELECT a_id FROM id RETURNING id, a_id)
                INSERT INTO a (id, b_id, name) SELECT a_id, id, 'app' FROM b_row;
                """);
        return ds;
    }

    private static DataSource dataSource(String database) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(ContainerFieldTest.PG.getJdbcUrl());
        ds.setDatabaseName(database);
        ds.setUser(ContainerFieldTest.PG.getUsername());
        ds.setPassword(ContainerFieldTest.PG.getPassword());
        return ds;
    }

    static final class ScenarioExtension implements ExecutionCondition, ParameterResolver {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ec) {
            return ec.getConfigurationParameter(SCENARIO).isPresent()
                    ? ConditionEvaluationResult.enabled("run by SupportMatrixTest")
                    : ConditionEvaluationResult.disabled("runs only inside SupportMatrixTest");
        }

        @Override
        public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
            return pc.getParameter().getType() == Scenario.class;
        }

        @Override
        public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
            return Scenario.valueOf(ec.getConfigurationParameter(SCENARIO).orElseThrow());
        }
    }

    @Transactional
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    abstract static class Exercise {

        final JdbcTemplate jdbc = new JdbcTemplate(DATA_SOURCE);

        Rowsmith rowsmith(Rowsmith parameter) {
            return parameter;
        }

        List<Row> fixtures() {
            return List.of();
        }

        @Test
        @Order(1)
        void rowsAreVisibleAndAFailedCreateChangesNothing(Rowsmith parameter, Scenario scenario) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isEqualTo(getClass().getEnclosingClass() == InSpringTransaction.class);
            Rowsmith rs = rowsmith(parameter);
            assertThatThrownBy(() -> rs.create(scenario.root, r -> r.set("name", REFUSED)))
                    .isInstanceOf(RowsmithException.class)
                    .hasMessageContaining(REFUSAL);
            Row row = rs.create(scenario.root);
            assertVisible(scenario, row);
            fixtures().forEach(fixture -> assertVisible(scenario, fixture));
            assertThat(count(scenario)).isEqualTo(APP_ROWS + fixtures().size() + 1);
            assertThatThrownBy(() -> jdbc.update(scenario.danglingInsert, MISSING_ID)).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @Order(2)
        void theFirstTestsRowsAreGoneAndTheClassFixturesStay(Rowsmith parameter, Scenario scenario) {
            assertThat(count(scenario)).isEqualTo(APP_ROWS + fixtures().size());
            fixtures().forEach(fixture -> assertVisible(scenario, fixture));
            assertVisible(scenario, rowsmith(parameter).create(scenario.root));
        }

        private void assertVisible(Scenario scenario, Row row) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + scenario.root + " WHERE id = ?", Long.class, row.id())).isOne();
        }

        private long count(Scenario scenario) {
            return jdbc.queryForObject("SELECT count(*) FROM " + scenario.root, Long.class);
        }
    }

    abstract static class FieldInjected extends Exercise {
        Rowsmith rowsmith;

        @Override
        Rowsmith rowsmith(Rowsmith parameter) {
            assertThat(rowsmith).isSameAs(parameter);
            return rowsmith;
        }
    }

    abstract static class ParameterInjected extends Exercise {}

    abstract static class ConstructorInjected extends Exercise {
        private final Rowsmith injected;

        ConstructorInjected(Rowsmith injected) {
            this.injected = injected;
        }

        @Override
        Rowsmith rowsmith(Rowsmith parameter) {
            assertThat(injected).isSameAs(parameter);
            return injected;
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    abstract static class PerClassInstance extends FieldInjected {
        final List<Row> fixtures = new ArrayList<>();

        @BeforeAll
        void createFixtures(Rowsmith classRowsmith, Scenario scenario) {
            fixtures.add(classRowsmith.create(scenario.root));
        }

        @Override
        List<Row> fixtures() {
            return fixtures;
        }
    }

    abstract static class ClassFixtures extends Exercise {
        static final List<Row> FIXTURES = new ArrayList<>();

        @BeforeAll
        static void createFixtures(Rowsmith classRowsmith, Scenario scenario) {
            FIXTURES.clear();
            FIXTURES.add(classRowsmith.create(scenario.root));
        }

        @Override
        List<Row> fixtures() {
            return FIXTURES;
        }
    }

    @RowsmithTest
    @ExtendWith(ScenarioExtension.class)
    abstract static class Mode {
        static final DataSource DATA_SOURCE = SupportMatrixTest.DATA_SOURCE;
    }

    @Configuration
    static class SpringConfig {
        @Bean
        DataSource dataSource() {
            return DATA_SOURCE;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
    }

    @SpringJUnitConfig(SpringConfig.class)
    static class InSpringTransaction extends Mode {
        @Nested class Field extends FieldInjected {}
        @Nested class Parameter extends ParameterInjected {}
        @Nested class Constructor extends ConstructorInjected { Constructor(Rowsmith rs) { super(rs); } }
        @Nested class PerClass extends PerClassInstance {}
        @Nested class BeforeAllFixtures extends ClassFixtures {}
    }

    @SpringJUnitConfig(SpringConfig.class)
    @TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
    static class InSpringWithoutTransaction extends Mode {
        @Nested class Field extends FieldInjected {}
        @Nested class Parameter extends ParameterInjected {}
        @Nested class Constructor extends ConstructorInjected { Constructor(Rowsmith rs) { super(rs); } }
        @Nested class PerClass extends PerClassInstance {}
        @Nested class BeforeAllFixtures extends ClassFixtures {}
    }

    static class WithoutTransaction extends Mode {
        @Nested class Field extends FieldInjected {}
        @Nested class Parameter extends ParameterInjected {}
        @Nested class Constructor extends ConstructorInjected { Constructor(Rowsmith rs) { super(rs); } }
        @Nested class PerClass extends PerClassInstance {}
        @Nested class BeforeAllFixtures extends ClassFixtures {}
    }
}
