package io.github.mundanej.mxjb.generator.gradle;

import io.github.mundanej.mxjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mxjb.generator.api.GeneratorProfile;
import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import io.github.mundanej.mxjb.generator.core.CoreGenerator;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Generates Java sources from explicit XSD inputs. */
@CacheableTask
public class MxjbGenerateTask extends DefaultTask {
  private final ConfigurableFileCollection schemas;
  private final DirectoryProperty outputDirectory;
  private final Property<String> profile;
  private final Property<String> defaultPackage;
  private final MapProperty<String, String> namespacePackages;
  private final ConfigurableFileCollection localRoots;
  private final MapProperty<String, String> catalogMappings;
  private final ConfigurableFileCollection catalogTargets;

  @Inject
  public MxjbGenerateTask(ObjectFactory objects) {
    this.schemas = objects.fileCollection();
    this.outputDirectory = objects.directoryProperty();
    this.profile = objects.property(String.class).convention("XP-DATA-10");
    this.defaultPackage =
        objects.property(String.class).convention(GeneratorRequest.DEFAULT_PACKAGE);
    this.namespacePackages = objects.mapProperty(String.class, String.class);
    this.localRoots = objects.fileCollection();
    this.catalogMappings = objects.mapProperty(String.class, String.class);
    this.catalogTargets = objects.fileCollection();
  }

  @TaskAction
  public void generate() {
    Path temporaryOutput = getTemporaryDir().toPath().resolve("generated");
    Path finalOutput = outputDirectory.get().getAsFile().toPath();
    try {
      deleteRecursively(temporaryOutput);
    } catch (IOException exception) {
      throw new GradleException(
          "Unable to prepare temporary generated source directory.", exception);
    }

    GeneratorRequest request =
        new GeneratorRequest(
            sortedPaths(schemas),
            temporaryOutput,
            profile(),
            defaultPackage.get(),
            sortedMap(namespacePackages.get()),
            sortedPaths(localRoots),
            catalogMappings());
    GeneratorResult result = new CoreGenerator().generate(request);
    if (!result.successful()) {
      throw new GradleException(formatDiagnostics(result.diagnostics()));
    }

    try {
      deleteRecursively(finalOutput);
      copyDirectory(temporaryOutput, finalOutput);
    } catch (IOException exception) {
      throw new GradleException("Unable to replace generated source directory.", exception);
    }
    for (Path generatedSource : result.generatedSources()) {
      getLogger().info("Generated {}", generatedSource);
    }
  }

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public ConfigurableFileCollection getSchemas() {
    return schemas;
  }

  @OutputDirectory
  public DirectoryProperty getOutputDirectory() {
    return outputDirectory;
  }

  @Input
  public Property<String> getProfile() {
    return profile;
  }

  @Input
  public Property<String> getDefaultPackage() {
    return defaultPackage;
  }

  @Input
  public MapProperty<String, String> getNamespacePackages() {
    return namespacePackages;
  }

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public ConfigurableFileCollection getLocalRoots() {
    return localRoots;
  }

  @Input
  public MapProperty<String, String> getCatalogMappings() {
    return catalogMappings;
  }

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public ConfigurableFileCollection getCatalogTargets() {
    return catalogTargets;
  }

  private GeneratorProfile profile() {
    String token = profile.get();
    return GeneratorProfile.fromCliToken(token)
        .orElseThrow(
            () ->
                new GradleException(
                    "GENERATOR_GRADLE_INVALID_ARGUMENT | profile | Unsupported generator profile "
                        + token
                        + "."));
  }

  private Map<URI, Path> catalogMappings() {
    TreeMap<URI, Path> sorted = new TreeMap<>(Comparator.comparing(URI::toString));
    for (Map.Entry<String, String> entry : catalogMappings.get().entrySet()) {
      sorted.put(URI.create(entry.getKey()), Path.of(entry.getValue()));
    }
    return Map.copyOf(sorted);
  }

  private List<Path> sortedPaths(ConfigurableFileCollection files) {
    return files.getFiles().stream()
        .map(file -> file.toPath().toAbsolutePath().normalize())
        .sorted()
        .toList();
  }

  private Map<String, String> sortedMap(Map<String, String> values) {
    return Map.copyOf(new TreeMap<>(values));
  }

  private String formatDiagnostics(List<GeneratorDiagnostic> diagnostics) {
    return String.join(
        System.lineSeparator(),
        diagnostics.stream().map(GeneratorDiagnostic::toManifestLine).toList());
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.createDirectories(target);
    if (!Files.exists(source)) {
      return;
    }
    try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
      for (Path path : paths.sorted().toList()) {
        Path relativePath = source.relativize(path);
        Path targetPath = target.resolve(relativePath);
        if (Files.isDirectory(path)) {
          Files.createDirectories(targetPath);
        } else {
          Path parent = targetPath.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          Files.copy(path, targetPath);
        }
      }
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (java.util.stream.Stream<Path> paths = Files.walk(path)) {
      for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(item);
      }
    }
  }
}
