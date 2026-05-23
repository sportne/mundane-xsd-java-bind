package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaIrIdentityConstraint;
import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.List;
import java.util.Objects;

/** Generated entry-point planning metadata for a global element. */
public record BindingRootElement(
    SchemaQName xmlName,
    BindingTypeReference type,
    BindingCardinality cardinality,
    List<SchemaIrIdentityConstraint> identityConstraints) {
  public BindingRootElement(
      SchemaQName xmlName, BindingTypeReference type, BindingCardinality cardinality) {
    this(xmlName, type, cardinality, List.of());
  }

  public BindingRootElement {
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(cardinality, "cardinality");
    identityConstraints =
        identityConstraints == null ? List.of() : List.copyOf(identityConstraints);
  }

  public String toText(String indent) {
    return indent
        + "root "
        + xmlName.toText()
        + " type="
        + type.toText()
        + " cardinality="
        + cardinality.toText()
        + (identityConstraints.isEmpty() ? "" : " identities=" + identityConstraints.size());
  }
}
