package io.github.mundanej.mxjb.generator.gradle;

import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/** Public Gradle plugin entry point for mxjb schema-to-Java generation. */
public final class MxjbGradlePlugin implements Plugin<Project> {
  public static final String EXTENSION_NAME = "mxjb";
  public static final String GENERATE_TASK_NAME = "generateMxjbSources";

  @Override
  public void apply(Project project) {
    MxjbExtension extension =
        project.getExtensions().create(EXTENSION_NAME, MxjbExtension.class, project.getObjects());
    extension
        .getOutputDirectory()
        .convention(project.getLayout().getBuildDirectory().dir("generated/sources/mxjb/java"));
    extension.getProfile().convention("XP-DATA-10");
    extension.getDefaultPackage().convention(GeneratorRequest.DEFAULT_PACKAGE);

    TaskProvider<MxjbGenerateTask> generateTask =
        project
            .getTasks()
            .register(
                GENERATE_TASK_NAME,
                MxjbGenerateTask.class,
                task -> {
                  task.setGroup("generation");
                  task.setDescription("Generates Java sources from configured XSD schemas.");
                  task.getSchemas().from(extension.getSchemas());
                  task.getOutputDirectory().convention(extension.getOutputDirectory());
                  task.getProfile().convention(extension.getProfile());
                  task.getDefaultPackage().convention(extension.getDefaultPackage());
                  task.getNamespacePackages().set(extension.getNamespacePackages());
                  task.getLocalRoots().from(extension.getLocalRoots());
                  task.getCatalogMappings().set(extension.getCatalogMappings());
                  task.getCatalogTargets().from(extension.getCatalogTargets());
                });

    project
        .getPlugins()
        .withType(JavaPlugin.class, plugin -> wireJavaSourceSet(project, generateTask));
  }

  private void wireJavaSourceSet(Project project, TaskProvider<MxjbGenerateTask> generateTask) {
    JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
    SourceSetContainer sourceSets = java.getSourceSets();
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    main.getJava().srcDir(generateTask.flatMap(MxjbGenerateTask::getOutputDirectory));
    project
        .getTasks()
        .named(main.getCompileJavaTaskName())
        .configure(task -> task.dependsOn(generateTask));
  }
}
