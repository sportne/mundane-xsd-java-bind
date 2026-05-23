package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized wildcard/open-content particle inside an ordered sequence. */
public record SchemaIrWildcard(
    SchemaCardinality cardinality,
    SchemaIrWildcardNamespace namespaceConstraint,
    String processContents)
    implements SchemaIrParticle {
  public SchemaIrWildcard(
      SchemaCardinality cardinality, SchemaIrWildcardNamespace namespaceConstraint) {
    this(cardinality, namespaceConstraint, "strict");
  }

  public SchemaIrWildcard {
    Objects.requireNonNull(cardinality, "cardinality");
    Objects.requireNonNull(namespaceConstraint, "namespaceConstraint");
    processContents =
        processContents == null || processContents.isBlank() ? "strict" : processContents;
  }

  @Override
  public String toText(String indent) {
    return indent
        + "wildcard namespace="
        + namespaceConstraint.toText()
        + " processContents="
        + processContents
        + " cardinality="
        + cardinality.toText();
  }
}
