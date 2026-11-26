package rowsmith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import rowsmith.internal.plan.ValuePattern;

/**
 * Project-wide settings for Rowsmith in code: per-column value patterns. Implement it
 * once and reference it from {@code @RowsmithTest(setup = MySetup.class)} or
 * {@link Rowsmith.Builder#setup(RowsmithSetup)}:
 *
 * <pre>{@code
 * public class TestData implements RowsmithSetup {
 *     public void configure(Config c) {
 *         c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com");
 *     }
 * }
 * }</pre>
 *
 * Implementations need a public no-arg constructor.
 */
public interface RowsmithSetup {

    void configure(Config config);

    /** No settings; the default of {@code @RowsmithTest(setup = ...)}. */
    final class None implements RowsmithSetup {
        @Override
        public void configure(Config config) {}
    }

    /** What a {@link RowsmithSetup} configures. */
    final class Config {
        private final List<ValuePattern> values = new ArrayList<>();

        Config() {}

        /**
         * Values for a column of every row Rowsmith generates, by row number (1, 2, ... per table and
         * Rowsmith instance). Values the test sets win; a value that breaks a constraint is an error.
         */
        public <T> Config value(Column<T> column, IntFunction<? extends T> byRowNumber) {
            values.add(new ValuePattern(column.table().qualifiedName(), column.name(), n -> column.toDb(byRowNumber.apply(n))));
            return this;
        }

        /** Like {@link #value(Column, IntFunction)} for a column named as in the database. */
        public Config value(String table, String column, IntFunction<?> byRowNumber) {
            values.add(new ValuePattern(table, column, byRowNumber));
            return this;
        }

        List<ValuePattern> values() { return Collections.unmodifiableList(values); }
    }
}
