package io.github.mundanej.mxjb.generator.core.bind;

import io.github.mundanej.mxjb.generator.core.schema.SchemaQName;
import java.util.Objects;

/** Generated entry-point planning metadata for a global element. */
public record BindingRootElement(
    SchemaQName xmlName, BindingTypeReference type, BindingCardinality cardinality) {
  public BindingRootElement {
    Objects.requireNonNull(xmlName, "xmlName");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(cardinality, "cardinality");
  }

  public String toText(String indent) {
    return indent
        + "root "
        + xmlName.toText()
        + " type="
        + type.toText()
        + " cardinality="
        + cardinality.toText();
  }
}
