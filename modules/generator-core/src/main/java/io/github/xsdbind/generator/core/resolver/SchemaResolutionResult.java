package io.github.xsdbind.generator.core.resolver;

import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import io.github.xsdbind.generator.core.schema.ResolvedSchemaManifest;
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
