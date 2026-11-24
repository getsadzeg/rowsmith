package io.github.rowsmith.junit;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.RowsmithSetup;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

/** See {@link RowsmithTest}. Can also be registered with {@code @ExtendWith(RowsmithExtension.class)}. */
public final class RowsmithExtension
        implements ParameterResolver, TestInstantiationAwareExtension, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(RowsmithExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return pc.getParameter().getType() == Rowsmith.class;
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        return rowsmith(ec);
    }

    @Override
    public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
        return ExtensionContextScope.TEST_METHOD;
    }

    @Override
    public void beforeEach(ExtensionContext ec) throws IllegalAccessException {
        for (Object instance : ec.getRequiredTestInstances().getAllInstances()) {
            for (Field f : rowsmithFields(instance)) {
                if (f.get(instance) == null) f.set(instance, rowsmith(ec));
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext ec) throws IllegalAccessException {
        Rowsmith rs = ownRowsmith(ec);
        if (rs == null) return;
        for (Object instance : ec.getRequiredTestInstances().getAllInstances()) {
            for (Field f : rowsmithFields(instance)) {
                if (f.get(instance) == rs) f.set(instance, null);
            }
        }
        cleanup(ec, rs);
    }

    @Override
    public void afterAll(ExtensionContext ec) {
        Rowsmith rs = ownRowsmith(ec);
        if (rs != null) cleanup(ec, rs);
    }

    Rowsmith rowsmith(ExtensionContext ec) {
        return ec.getStore(NS).getOrComputeIfAbsent(ec.getUniqueId(), k -> create(ec), Rowsmith.class);
    }

    private static Rowsmith ownRowsmith(ExtensionContext ec) {
        return ec.getStore(NS).get(ec.getUniqueId(), Rowsmith.class);
    }

    private void cleanup(ExtensionContext ec, Rowsmith rs) {
        if (settings(ec).map(RowsmithTest::cleanup).orElse(true)) rs.cleanup();
    }

    private static List<Field> rowsmithFields(Object instance) {
        List<Field> fields = ReflectionSupport.findFields(instance.getClass(),
                f -> f.getType() == Rowsmith.class && !Modifier.isStatic(f.getModifiers()), HierarchyTraversalMode.TOP_DOWN);
        for (Field f : fields) f.setAccessible(true);
        return fields;
    }

    private Optional<RowsmithTest> settings(ExtensionContext ec) {
        return AnnotationSupport.findAnnotation(ec.getRequiredTestClass(), RowsmithTest.class, ec.getEnclosingTestClasses());
    }

    private Rowsmith create(ExtensionContext ec) {
        Class<? extends RowsmithSetup> setup = settings(ec).<Class<? extends RowsmithSetup>>map(RowsmithTest::setup)
                .orElse(RowsmithSetup.None.class);
        Rowsmith.Builder b = Rowsmith.builder(dataSource(ec)).setup(ReflectionSupport.newInstance(setup));
        return b.build();
    }

    DataSource dataSource(ExtensionContext ec) {
        boolean containers = ContainerSupport.available();
        for (Class<?> testClass : innermostFirst(ec)) {
            Object instance = ec.getTestInstances().flatMap(i -> i.findInstance(testClass)).orElse(null);
            for (Field f : ReflectionSupport.findFields(testClass, f -> DataSource.class.isAssignableFrom(f.getType()), HierarchyTraversalMode.TOP_DOWN)) {
                if (read(f, instance) instanceof DataSource ds) return ds;
            }
            if (!containers) continue;
            for (Field f : ReflectionSupport.findFields(testClass, f -> ContainerSupport.isContainer(f.getType()), HierarchyTraversalMode.TOP_DOWN)) {
                Object container = read(f, instance);
                if (container != null) return ContainerSupport.dataSource(container);
            }
        }
        throw new RowsmithException(ErrorCode.NO_DATASOURCE, "no DataSource for " + ec.getRequiredTestClass().getSimpleName()
                + ". Use a DataSource field or a Testcontainers "
                + "PostgreSQLContainer field (static @Container) in the test class.");
    }

    private static List<Class<?>> innermostFirst(ExtensionContext ec) {
        List<Class<?>> classes = new ArrayList<>(ec.getEnclosingTestClasses());
        classes.add(ec.getRequiredTestClass());
        Collections.reverse(classes);
        return classes;
    }

    private static Object read(Field f, Object instance) {
        try {
            f.setAccessible(true);
            if (Modifier.isStatic(f.getModifiers())) return f.get(null);
            return instance == null ? null : f.get(instance);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
