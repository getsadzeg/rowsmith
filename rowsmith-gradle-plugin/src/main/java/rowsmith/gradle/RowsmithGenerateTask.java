package rowsmith.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import rowsmith.codegen.Codegen;
import rowsmith.codegen.FlywaySource;
import rowsmith.codegen.SchemaSource;
import rowsmith.codegen.SqlScriptsSource;

/** Generates the typed constants class from the schema; runs again only when its inputs change. */
@DisableCachingByDefault(because = "Runs a database; regenerating is cheap next to sharing the output through a build cache")
public abstract class RowsmithGenerateTask extends DefaultTask {

    @Input
    public abstract Property<SchemaSourceType> getSource();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getMigrations();

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract Property<String> getClassName();

    @Input
    public abstract Property<String> getPostgresImage();

    @Input
    @Optional
    public abstract Property<String> getUrl();

    @Input
    @Optional
    public abstract Property<String> getUser();

    @Internal
    public abstract Property<String> getPassword();

    @Input
    @Optional
    public abstract Property<String> getCustomSource();

    @Classpath
    public abstract ConfigurableFileCollection getCustomClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @TaskAction
    public void generate() throws Exception {
        Path out = getOutputDir().get().getAsFile().toPath();
        getFileSystemOperations().delete(spec -> spec.delete(out.toFile()));
        Codegen codegen = new Codegen(getPackageName().get(), getClassName().get());
        String image = getPostgresImage().get();
        switch (getSource().get()) {
            case FLYWAY -> codegen.generate(new FlywaySource(flywayLocations()), image, out);
            case SQL -> codegen.generate(new SqlScriptsSource(scripts()), image, out);
            case URL -> codegen.generate(getUrl().get(), getUser().getOrNull(), getPassword().getOrNull(), out);
            case CUSTOM -> {
                try (URLClassLoader loader = customClassLoader()) {
                    codegen.generate(customSource(loader), image, out);
                }
            }
        }
    }

    private List<String> flywayLocations() {
        List<String> locations = new ArrayList<>();
        for (File dir : getMigrations().getFiles()) locations.add("filesystem:" + dir.getAbsolutePath());
        return locations;
    }

    private List<Path> scripts() throws IOException {
        List<Path> scripts = new ArrayList<>();
        for (File f : getMigrations().getFiles()) {
            if (!f.isDirectory()) {
                scripts.add(f.toPath());
                continue;
            }
            try (Stream<Path> files = Files.walk(f.toPath())) {
                files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".sql")).sorted().forEach(scripts::add);
            }
        }
        return scripts;
    }

    private URLClassLoader customClassLoader() throws IOException {
        List<URL> urls = new ArrayList<>();
        for (File f : getCustomClasspath().getFiles()) urls.add(f.toURI().toURL());
        return new URLClassLoader(urls.toArray(URL[]::new), SchemaSource.class.getClassLoader());
    }

    private SchemaSource customSource(ClassLoader loader) throws ReflectiveOperationException {
        String name = getCustomSource().get();
        Class<?> type = Class.forName(name, true, loader);
        if (!SchemaSource.class.isAssignableFrom(type)) {
            throw new GradleException("rowsmith.custom(\"" + name + "\", ...): it does not implement " + SchemaSource.class.getName());
        }
        return (SchemaSource) type.getDeclaredConstructor().newInstance();
    }
}
