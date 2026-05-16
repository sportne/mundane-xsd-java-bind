package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Binding model build result. */
public record BindingResult(BindingModel model, List<SchemaDiagnostic> diagnostics) {
  public BindingResult {
    diagnostics = List.copyOf(diagnostics);
  }

  public static BindingResult empty(List<SchemaDiagnostic> diagnostics) {
    return new BindingResult(BindingModel.empty(), diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
