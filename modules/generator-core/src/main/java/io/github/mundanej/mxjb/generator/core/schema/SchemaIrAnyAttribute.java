package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized XSD 1.0 anyAttribute wildcard use. */
public record SchemaIrAnyAttribute(
    SchemaIrWildcardNamespace namespaceConstraint, String processContents) {
  public SchemaIrAnyAttribute {
    Objects.requireNonNull(namespaceConstraint, "namespaceConstraint");
    processContents =
        processContents == null || processContents.isBlank() ? "strict" : processContents;
  }

  public String toText(String indent) {
    return indent
        + "anyAttribute namespace="
        + namespaceConstraint.toText()
        + " processContents="
        + processContents;
  }
}
