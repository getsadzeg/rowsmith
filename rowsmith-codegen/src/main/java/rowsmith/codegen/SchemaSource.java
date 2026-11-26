package rowsmith.codegen;

import javax.sql.DataSource;

/**
 * Builds the schema in an empty, temporary Postgres database so the code generator can read it.
 * Built in: {@link FlywaySource} and {@link SqlScriptsSource}. A custom implementation needs a public
 * no-arg constructor (e.g. to run Liquibase or the application's own schema setup).
 */
@FunctionalInterface
public interface SchemaSource {

    void apply(DataSource dataSource) throws Exception;
}
