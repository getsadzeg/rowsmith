package io.github.rowsmith.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * {@code id("io.github.getsadzeg.rowsmith")}: adds the {@code rowsmithGenerate} task, which writes the
 * typed constants class to {@code build/generated/rowsmith/java} and adds it to the test source set,
 * so {@code compileTestJava} runs it first.
 */
public class RowsmithPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        RowsmithExtension ext = project.getExtensions().create("rowsmith", RowsmithExtension.class);
        ext.getClassName().convention("Db");
        ext.getPostgresImage().convention("postgres:17-alpine");
        ext.getSource().convention(SchemaSourceType.FLYWAY);
        ext.getMigrations().convention(ext.defaultMigrations());

        TaskProvider<RowsmithGenerateTask> generate = project.getTasks().register("rowsmithGenerate", RowsmithGenerateTask.class, t -> {
            t.setGroup("build");
            t.setDescription("Generates Rowsmith's typed table and column constants from the database schema.");
            t.getSource().set(ext.getSource());
            t.getMigrations().from(ext.getMigrations());
            t.getPackageName().set(ext.getPackageName());
            t.getClassName().set(ext.getClassName());
            t.getPostgresImage().set(ext.getPostgresImage());
            t.getUrl().set(ext.getUrl());
            t.getUser().set(ext.getUser());
            t.getPassword().set(ext.getPassword());
            t.getCustomSource().set(ext.getCustomSource());
            t.getCustomClasspath().from(ext.getCustomClasspath());
            t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/rowsmith/java"));
            t.getOutputs().upToDateWhen(task -> schemaComesFromTrackedInputs((RowsmithGenerateTask) task));
        });

        project.getPlugins().withType(JavaPlugin.class, java -> project.getExtensions().getByType(SourceSetContainer.class)
                .getByName(SourceSet.TEST_SOURCE_SET_NAME).getJava().srcDir(generate.flatMap(RowsmithGenerateTask::getOutputDir)));
    }

    private static boolean schemaComesFromTrackedInputs(RowsmithGenerateTask task) {
        return task.getSource().get() != SchemaSourceType.URL;
    }
}
