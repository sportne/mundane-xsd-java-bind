package io.github.mundanej.mxjb.generator.core.resolver;

import io.github.mundanej.mxjb.generator.core.diagnostics.SchemaDiagnostic;
import io.github.mundanej.mxjb.generator.core.schema.ResolvedSchemaManifest;
import java.util.List;

/** Result of applying schema resolver policy to a primary schema URI. */
public record SchemaResolutionResult(
    ResolvedSchemaManifest manifest, List<SchemaDiagnostic> diagnostics) {
  public SchemaResolutionResult {
    diagnostics = List.copyOf(diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
