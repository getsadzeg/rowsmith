package io.github.rowsmith.junit;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.RowsmithException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

final class SpringSupport {

    private SpringSupport() {}

    static boolean available() {
        try {
            Class.forName("org.springframework.test.context.junit.jupiter.SpringExtension", false, SpringSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static boolean isSpringTest(ExtensionContext ec) {
        return Stream.concat(ec.getEnclosingTestClasses().stream(), Stream.of(ec.getRequiredTestClass()))
                .flatMap(c -> AnnotationSupport.findRepeatableAnnotations(c, ExtendWith.class).stream())
                .anyMatch(e -> Arrays.asList(e.value()).contains(SpringExtension.class));
    }

    private static Optional<ApplicationContext> context(ExtensionContext ec) {
        if (!isSpringTest(ec)) return Optional.empty();
        return Optional.of(SpringExtension.getApplicationContext(ec));
    }

    static Optional<DataSource> dataSource(ExtensionContext ec) {
        return context(ec).flatMap(SpringSupport::dataSource);
    }

    static Optional<DataSource> dataSource(ApplicationContext ctx) {
        return primaryBean(ctx, DataSource.class, ErrorCode.NO_DATASOURCE);
    }

    static Optional<Clock> clock(ExtensionContext ec) {
        return context(ec).flatMap(SpringSupport::clock);
    }

    static Optional<Clock> clock(ApplicationContext ctx) {
        return primaryBean(ctx, Clock.class, ErrorCode.INVALID_CONFIG);
    }

    private static <T> Optional<T> primaryBean(ApplicationContext ctx, Class<T> type, ErrorCode ambiguous) {
        String[] names = ctx.getBeanNamesForType(type);
        if (names.length == 0) return Optional.empty();
        T unique = ctx.getBeanProvider(type).getIfUnique();
        if (unique == null) {
            throw new RowsmithException(ambiguous, "the Spring context has several " + type.getSimpleName() + " beans ("
                    + String.join(", ", names) + ") and none is @Primary; mark the one Rowsmith should use as @Primary");
        }
        return Optional.of(unique);
    }
}
