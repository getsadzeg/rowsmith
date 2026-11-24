package io.github.rowsmith.junit;

import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.RowsmithSetup;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Injects a {@link Rowsmith} (as a test method or constructor parameter, or into a field of that type)
 * bound to the test's DataSource: the Spring context's DataSource, a {@code DataSource} field, or a
 * Testcontainers JDBC container field. Each test gets its own Rowsmith, constructor parameters included; a
 * Rowsmith resolved for a {@code @BeforeAll} method or for the constructor of a {@code PER_CLASS} test instance
 * belongs to the class and keeps its rows until all its tests have run.
 * Rows join an active Spring test transaction (rolled back); rows created outside one are deleted after
 * the test, or after the class for the class's Rowsmith (validated first, never half-deleted).
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(RowsmithExtension.class)
public @interface RowsmithTest {

    /** Per-column value patterns for every test of the class; see {@link RowsmithSetup}. */
    Class<? extends RowsmithSetup> setup() default RowsmithSetup.None.class;

    /** Delete rows Rowsmith created outside a rolled-back transaction after each test (and after the class). */
    boolean cleanup() default true;
}
