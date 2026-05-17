package io.github.mundanej.mxjb.generator.core.emit;

import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Generated XML validator source emission result. */
public record GeneratedValidatorEmissionResult(
    List<GeneratedJavaSource> sources, List<SchemaDiagnostic> diagnostics) {
  public GeneratedValidatorEmissionResult {
    sources = List.copyOf(sources);
    diagnostics = List.copyOf(diagnostics);
  }

  public static GeneratedValidatorEmissionResult empty(List<SchemaDiagnostic> diagnostics) {
    return new GeneratedValidatorEmissionResult(List.of(), diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
