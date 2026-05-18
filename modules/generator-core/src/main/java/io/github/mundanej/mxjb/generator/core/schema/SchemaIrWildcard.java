package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized wildcard/open-content particle inside an ordered sequence. */
public record SchemaIrWildcard(
    SchemaCardinality cardinality, SchemaIrWildcardNamespace namespaceConstraint)
    implements SchemaIrParticle {
  public SchemaIrWildcard {
    Objects.requireNonNull(cardinality, "cardinality");
    Objects.requireNonNull(namespaceConstraint, "namespaceConstraint");
  }

  @Override
  public String toText(String indent) {
    return indent
        + "wildcard namespace="
        + namespaceConstraint.toText()
        + " cardinality="
        + cardinality.toText();
  }
}
