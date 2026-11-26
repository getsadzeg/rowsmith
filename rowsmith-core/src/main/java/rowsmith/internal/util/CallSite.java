package rowsmith.internal.util;

import java.lang.StackWalker.StackFrame;
import java.security.CodeSource;
import java.util.List;
import java.util.Objects;

public final class CallSite {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final List<String> FRAMEWORK_PACKAGES = List.of("org.junit.", "org.springframework.");
    private static final CodeSource ROWSMITH = CallSite.class.getProtectionDomain().getCodeSource();

    private CallSite() {}

    public static String find() {
        return WALKER.walk(frames -> frames.filter(CallSite::isCaller).findFirst()).map(CallSite::render).orElse("");
    }

    private static boolean isCaller(StackFrame frame) {
        Class<?> c = frame.getDeclaringClass();
        if (isJdk(c) || sameSource(c.getProtectionDomain().getCodeSource(), ROWSMITH)) return false;
        return FRAMEWORK_PACKAGES.stream().noneMatch(c.getName()::startsWith);
    }

    private static boolean isJdk(Class<?> c) {
        ClassLoader loader = c.getClassLoader();
        return loader == null || loader == ClassLoader.getPlatformClassLoader();
    }

    private static boolean sameSource(CodeSource a, CodeSource b) {
        return a != null && b != null && Objects.equals(location(a), location(b));
    }

    private static String location(CodeSource source) {
        return source.getLocation() == null ? null : source.getLocation().toExternalForm();
    }

    private static String render(StackFrame frame) {
        Class<?> c = frame.getDeclaringClass();
        while (c.getEnclosingClass() != null) c = c.getEnclosingClass();
        return frame.getLineNumber() > 0 ? "(" + c.getSimpleName() + ":" + frame.getLineNumber() + ")" : "(" + c.getSimpleName() + ")";
    }
}
