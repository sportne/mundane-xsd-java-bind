package io.github.xsdbind.generator.core.schema;

import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Result of building the component graph and normalized schema IR. */
public record SchemaIrResult(
    SchemaComponentGraph graph, SchemaIrModel model, List<SchemaDiagnostic> diagnostics) {
  public SchemaIrResult {
    diagnostics = List.copyOf(diagnostics);
  }

  public static SchemaIrResult empty(List<SchemaDiagnostic> diagnostics) {
    return new SchemaIrResult(SchemaComponentGraph.empty(), SchemaIrModel.empty(), diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
