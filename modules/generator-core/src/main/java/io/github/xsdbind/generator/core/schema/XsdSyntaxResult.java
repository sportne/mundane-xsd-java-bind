package io.github.xsdbind.generator.core.schema;

import io.github.xsdbind.generator.core.diagnostics.SchemaDiagnostic;
import java.util.List;

/** Result of parsing resolver-approved schemas into the raw syntax model. */
public record XsdSyntaxResult(XsdSyntaxModel model, List<SchemaDiagnostic> diagnostics) {
  public XsdSyntaxResult {
    diagnostics = List.copyOf(diagnostics);
  }

  public boolean hasErrors() {
    return !diagnostics.isEmpty();
  }
}
