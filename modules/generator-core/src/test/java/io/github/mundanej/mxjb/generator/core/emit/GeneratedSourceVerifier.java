package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Shared test harness for generated Java source verification. */
final class GeneratedSourceVerifier {
  private final Path tempDirectory;
  private int compileIndex;

  GeneratedSourceVerifier(Path tempDirectory) {
    this.tempDirectory = Objects.requireNonNull(tempDirectory, "tempDirectory");
  }

  void assertGoldenSources(String resourceRoot, List<GeneratedJavaSource> sources)
      throws IOException {
    assertUniqueRelativePaths(sources);
    for (GeneratedJavaSource source : sources) {
      String resourcePath = resourceRoot + "/" + resourcePath(source.relativePath()) + ".golden";
      String expected = readResource(resourcePath, source.relativePath());
      assertEquals(
          expected, source.sourceText(), "Golden source mismatch for " + source.relativePath());
    }
  }

  void assertDeterministic(List<GeneratedJavaSource> first, List<GeneratedJavaSource> second) {
    assertEquals(first, second, "Generated source emission must be deterministic.");
  }

  CompiledSources compile(List<GeneratedJavaSource> sources) throws IOException {
    assertUniqueRelativePaths(sources);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "A JDK with JavaCompiler is required for generated source tests.");
    Path workDirectory = tempDirectory.resolve("generated-source-compile-" + compileIndex);
    compileIndex++;
    Path sourceRoot = workDirectory.resolve("source");
    Path classRoot = workDirectory.resolve("classes");
    Files.createDirectories(sourceRoot);
    Files.createDirectories(classRoot);
    for (GeneratedJavaSource source : sources) {
      Path sourcePath = sourceRoot.resolve(source.relativePath());
      Path sourceParent = sourcePath.getParent();
      if (sourceParent != null) {
        Files.createDirectories(sourceParent);
      }
      Files.writeString(sourcePath, source.sourceText(), StandardCharsets.UTF_8);
    }
    List<String> sourceArguments =
        sources.stream()
            .map(source -> sourceRoot.resolve(source.relativePath()).toString())
            .toList();
    List<String> compilerArguments =
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    "--release",
                    "21",
                    "-Xlint:all",
                    "-Werror",
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-d",
                    classRoot.toString()),
                sourceArguments.stream())
            .toList();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    int exitCode = compiler.run(null, output, output, compilerArguments.toArray(String[]::new));
    if (exitCode != 0) {
      fail(
          "Generated source compilation failed with exit code "
              + exitCode
              + System.lineSeparator()
              + output.toString(StandardCharsets.UTF_8));
    }
    return new CompiledSources(
        new URLClassLoader(new URL[] {classRoot.toUri().toURL()}, getClass().getClassLoader()));
  }

  private void assertUniqueRelativePaths(List<GeneratedJavaSource> sources) {
    Set<Path> paths = new LinkedHashSet<>();
    for (GeneratedJavaSource source : sources) {
      if (!paths.add(source.relativePath())) {
        fail("Duplicate generated source path: " + source.relativePath());
      }
    }
  }

  private String readResource(String resourcePath, Path relativePath) throws IOException {
    InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (resourceStream == null) {
      throw new AssertionError("Missing golden source for " + relativePath + " at " + resourcePath);
    }
    try (InputStream stream = resourceStream) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String resourcePath(Path path) {
    return path.toString().replace('\\', '/');
  }

  record CompiledSources(URLClassLoader loader) implements AutoCloseable {
    Class<?> load(String className) throws ClassNotFoundException {
      return loader.loadClass(className);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }
}
