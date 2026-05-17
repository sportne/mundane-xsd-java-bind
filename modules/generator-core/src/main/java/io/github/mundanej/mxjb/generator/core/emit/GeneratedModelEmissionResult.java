package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Generated model source emission result. */
public record GeneratedModelEmissionResult(
    List<GeneratedJavaSource> sources, List<SchemaDiagnostic> diagnostics) {
  public GeneratedModelEmissionResult {
    sources = List.copyOf(sources);
    diagnostics = List.copyOf(diagnostics);
  }

  public static GeneratedModelEmissionResult empty(List<SchemaDiagnostic> diagnostics) {
    return new GeneratedModelEmissionResult(List.of(), diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
