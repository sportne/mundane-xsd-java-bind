package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Generated XML reader source emission result. */
public record GeneratedReaderEmissionResult(
    List<GeneratedJavaSource> sources, List<SchemaDiagnostic> diagnostics) {
  public GeneratedReaderEmissionResult {
    sources = List.copyOf(sources);
    diagnostics = List.copyOf(diagnostics);
  }

  public static GeneratedReaderEmissionResult empty(List<SchemaDiagnostic> diagnostics) {
    return new GeneratedReaderEmissionResult(List.of(), diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
