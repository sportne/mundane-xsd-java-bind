package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Generated XML writer source emission result. */
public record GeneratedWriterEmissionResult(
    List<GeneratedJavaSource> sources, List<SchemaDiagnostic> diagnostics) {
  public GeneratedWriterEmissionResult {
    sources = List.copyOf(sources);
    diagnostics = List.copyOf(diagnostics);
  }

  public static GeneratedWriterEmissionResult empty(List<SchemaDiagnostic> diagnostics) {
    return new GeneratedWriterEmissionResult(List.of(), diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
