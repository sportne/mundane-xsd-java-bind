package io.github.mundanej.mxjb.generator.core.emit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mxjb.generator.core.bind.BindingJavaName;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GeneratedSourceVerifierTest {
  private static final String GOLDEN_ROOT =
      "io/github/mundanej/mxjb/generator/core/emit/golden/harness-match";

  @TempDir private Path tempDirectory;

  @Test
  void goldenMatchPassesForApprovedSource() throws IOException {
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);
    GeneratedJavaSource source =
        source(
            "com.example.Hello",
            Path.of("com/example/Hello.java"),
            """
            package com.example;

            public final class Hello {}
            """);

    verifier.assertGoldenSources(GOLDEN_ROOT, List.of(source));
  }

  @Test
  void goldenMismatchReportsRelativePath() {
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);
    GeneratedJavaSource source =
        source(
            "com.example.Hello",
            Path.of("com/example/Hello.java"),
            """
            package com.example;

            public final class Hello {
            }
            """);

    AssertionError error =
        assertThrows(
            AssertionError.class, () -> verifier.assertGoldenSources(GOLDEN_ROOT, List.of(source)));

    assertTrue(error.getMessage().contains("com/example/Hello.java"));
  }

  @Test
  void duplicateRelativePathsFailBeforeCompilation() {
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);
    GeneratedJavaSource first =
        source("com.example.First", Path.of("com/example/Duplicate.java"), "class First {}");
    GeneratedJavaSource second =
        source("com.example.Second", Path.of("com/example/Duplicate.java"), "class Second {}");

    AssertionError error =
        assertThrows(AssertionError.class, () -> verifier.compile(List.of(first, second)));

    assertTrue(error.getMessage().contains("Duplicate generated source path"));
    assertTrue(error.getMessage().contains("com/example/Duplicate.java"));
  }

  @Test
  void compileFailureReportsCompilerOutput() {
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);
    GeneratedJavaSource source =
        source(
            "com.example.Broken",
            Path.of("com/example/Broken.java"),
            """
            package com.example;

            public final class Broken {
              public void broken(
            }
            """);

    AssertionError error =
        assertThrows(AssertionError.class, () -> verifier.compile(List.of(source)));

    assertTrue(error.getMessage().contains("Generated source compilation failed with exit code"));
    assertTrue(error.getMessage().contains("Broken.java"));
  }

  @Test
  void deterministicComparisonUsesSourceListAndText() {
    GeneratedSourceVerifier verifier = new GeneratedSourceVerifier(tempDirectory);
    GeneratedJavaSource source =
        source(
            "com.example.Hello",
            Path.of("com/example/Hello.java"),
            """
            package com.example;

            public final class Hello {}
            """);

    verifier.assertDeterministic(List.of(source), List.of(source));

    AssertionError error =
        assertThrows(
            AssertionError.class,
            () ->
                verifier.assertDeterministic(
                    List.of(source),
                    List.of(
                        source(
                            "com.example.Hello",
                            Path.of("com/example/Hello.java"),
                            "package com.example; public final class Hello { private Hello() {} }"))));

    assertTrue(error.getMessage().contains("Generated source emission must be deterministic"));
  }

  private GeneratedJavaSource source(String qualifiedName, Path path, String sourceText) {
    int separator = qualifiedName.lastIndexOf('.');
    return new GeneratedJavaSource(
        new BindingJavaName(
            qualifiedName.substring(0, separator), qualifiedName.substring(separator + 1)),
        path,
        sourceText);
  }
}
